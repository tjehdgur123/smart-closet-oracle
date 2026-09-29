package com.example.smartcloset.server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.concurrent.Executors;

public final class ServerMain {
    private static final Gson JSON = new Gson();
    private static final int MAX_BODY_BYTES = 10 * 1024 * 1024;
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;

    public static void main(String[] args) throws Exception {
        dbUrl = env("SMART_CLOSET_DB_URL", "jdbc:oracle:thin:@localhost:1521/XEPDB1");
        dbUser = System.getenv("SMART_CLOSET_DB_USER");
        dbPassword = System.getenv("SMART_CLOSET_DB_PASSWORD");
        if (dbUser == null || dbPassword == null) {
            throw new IllegalStateException("Set SMART_CLOSET_DB_USER and SMART_CLOSET_DB_PASSWORD first.");
        }
        Class.forName("oracle.jdbc.OracleDriver");
        try (Connection connection = connect();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("SELECT 1 FROM dual")) {
            result.next();
        }
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT id FROM clothes WHERE 1 = 0").close();
            statement.executeQuery("SELECT id FROM coordination WHERE 1 = 0").close();
            statement.executeQuery("SELECT coordination_id FROM coordination_item WHERE 1 = 0").close();
        }
        int port = Integer.parseInt(env("SMART_CLOSET_PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", ServerMain::handle);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("Smart Closet Oracle API ready on http://127.0.0.1:" + port);
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String[] path = exchange.getRequestURI().getPath().split("/");
            if (path.length == 2 && "health".equals(path[1]) && "GET".equals(method)) {
                try (Connection db = connect(); Statement statement = db.createStatement();
                     ResultSet result = statement.executeQuery("SELECT 1 FROM dual")) {
                    result.next();
                    sendJson(exchange, 200, "{\"database\":\"oracle\",\"connected\":true}");
                }
            } else if (path.length == 2 && "clothes".equals(path[1])) {
                if ("GET".equals(method)) {
                    listClothes(exchange);
                } else if ("POST".equals(method)) {
                    createClothes(exchange);
                } else {
                    sendJson(exchange, 405, "{}");
                }
            } else if (path.length >= 3 && "clothes".equals(path[1])) {
                int id = parseId(path[2]);
                if (path.length == 4 && "image".equals(path[3]) && "GET".equals(method)) {
                    getImage(exchange, id);
                } else if (path.length == 4 && "favorite".equals(path[3]) && "PUT".equals(method)) {
                    setFavorite(exchange, "clothes", id, readJson(exchange));
                } else if (path.length == 3 && "DELETE".equals(method)) {
                    deleteClothes(exchange, id);
                } else {
                    sendJson(exchange, 405, "{}");
                }
            } else if (path.length == 2 && "outfits".equals(path[1])) {
                if ("GET".equals(method)) {
                    listOutfits(exchange);
                } else if ("POST".equals(method)) {
                    createOutfit(exchange);
                } else {
                    sendJson(exchange, 405, "{}");
                }
            } else if (path.length == 4 && "outfits".equals(path[1])
                    && "favorite".equals(path[3]) && "PUT".equals(method)) {
                setFavorite(exchange, "coordination", parseId(path[2]), readJson(exchange));
            } else {
                sendJson(exchange, 404, "{}");
            }
        } catch (IllegalArgumentException e) {
            sendJson(exchange, 400, "{\"error\":\"Invalid request\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendJson(exchange, 500, "{\"error\":\"Database operation failed\"}");
        } finally {
            exchange.close();
        }
    }

    private static int parseId(String raw) {
        int id = Integer.parseInt(raw);
        if (id <= 0) throw new IllegalArgumentException("Invalid id");
        return id;
    }

    private static JsonObject readJson(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) throw new IllegalArgumentException("Body too large");
        return JsonParser.parseString(new String(body, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private static void listClothes(HttpExchange exchange) throws Exception {
        JsonArray items = new JsonArray();
        try (Connection db = connect(); PreparedStatement sql = db.prepareStatement(
                "SELECT id, category, color, season, is_favorite FROM clothes ORDER BY id DESC");
             ResultSet rows = sql.executeQuery()) {
            while (rows.next()) {
                JsonObject item = new JsonObject();
                item.addProperty("id", rows.getInt("id"));
                item.addProperty("category", rows.getString("category"));
                item.addProperty("color", rows.getString("color"));
                item.addProperty("season", rows.getString("season"));
                item.addProperty("isFavorite", rows.getInt("is_favorite") == 1);
                items.add(item);
            }
        }
        sendJson(exchange, 200, JSON.toJson(items));
    }

    private static void createClothes(HttpExchange exchange) throws Exception {
        JsonObject input = readJson(exchange);
        String category = required(input, "category", 40);
        String color = required(input, "color", 40);
        String season = required(input, "season", 40);
        byte[] image = Base64.getDecoder().decode(required(input, "imageBase64", MAX_BODY_BYTES));
        if (image.length == 0 || image.length > 6 * 1024 * 1024) throw new IllegalArgumentException("Invalid image");
        String mime = required(input, "imageMime", 80);
        if (!mime.startsWith("image/")) throw new IllegalArgumentException("Invalid image type");
        String importKey = input.has("importKey") ? required(input, "importKey", 120) : null;
        try (Connection db = connect(); PreparedStatement sql = db.prepareStatement(
                "INSERT INTO clothes(category, color, season, is_favorite, import_key, image_mime, image_data) VALUES (?, ?, ?, ?, ?, ?, ?)",
                new String[]{"ID"})) {
            Integer existingId = importedId(db, "clothes", importKey);
            if (existingId != null) {
                sendJson(exchange, 200, "{\"id\":" + existingId + "}");
                return;
            }
            sql.setString(1, category);
            sql.setString(2, color);
            sql.setString(3, season);
            sql.setInt(4, input.has("isFavorite") && input.get("isFavorite").getAsBoolean() ? 1 : 0);
            sql.setString(5, importKey);
            sql.setString(6, mime);
            sql.setBytes(7, image);
            sql.executeUpdate();
            try (ResultSet keys = sql.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("No generated id");
                sendJson(exchange, 201, "{\"id\":" + keys.getInt(1) + "}");
            }
        }
    }

    private static void getImage(HttpExchange exchange, int id) throws Exception {
        try (Connection db = connect(); PreparedStatement sql = db.prepareStatement(
                "SELECT image_mime, image_data FROM clothes WHERE id = ?")) {
            sql.setInt(1, id);
            try (ResultSet row = sql.executeQuery()) {
                if (!row.next()) {
                    sendJson(exchange, 404, "{}");
                    return;
                }
                byte[] image = row.getBytes("image_data");
                exchange.getResponseHeaders().set("Content-Type", row.getString("image_mime"));
                exchange.sendResponseHeaders(200, image.length);
                exchange.getResponseBody().write(image);
            }
        }
    }

    private static void setFavorite(HttpExchange exchange, String table, int id, JsonObject input) throws Exception {
        String sqlText = "UPDATE " + table + " SET is_favorite = ? WHERE id = ?";
        try (Connection db = connect(); PreparedStatement sql = db.prepareStatement(sqlText)) {
            sql.setInt(1, input.get("isFavorite").getAsBoolean() ? 1 : 0);
            sql.setInt(2, id);
            sendJson(exchange, sql.executeUpdate() == 1 ? 200 : 404, "{}");
        }
    }

    private static void deleteClothes(HttpExchange exchange, int id) throws Exception {
        try (Connection db = connect()) {
            db.setAutoCommit(false);
            try (PreparedStatement delete = db.prepareStatement("DELETE FROM clothes WHERE id = ?");
                 Statement cleanup = db.createStatement()) {
                delete.setInt(1, id);
                int count = delete.executeUpdate();
                cleanup.executeUpdate("DELETE FROM coordination c WHERE NOT EXISTS "
                        + "(SELECT 1 FROM coordination_item i WHERE i.coordination_id = c.id)");
                db.commit();
                sendJson(exchange, count == 1 ? 200 : 404, "{}");
            } catch (Exception e) {
                db.rollback();
                throw e;
            }
        }
    }

    private static void listOutfits(HttpExchange exchange) throws Exception {
        JsonArray outfits = new JsonArray();
        try (Connection db = connect(); PreparedStatement sql = db.prepareStatement(
                "SELECT c.id, c.name, c.is_favorite, i.clothes_id FROM coordination c "
                        + "LEFT JOIN coordination_item i ON i.coordination_id = c.id "
                        + "ORDER BY c.id DESC, i.display_order"); ResultSet rows = sql.executeQuery()) {
            JsonObject current = null;
            int currentId = -1;
            while (rows.next()) {
                int id = rows.getInt("id");
                if (id != currentId) {
                    current = new JsonObject();
                    current.addProperty("id", id);
                    current.addProperty("name", rows.getString("name"));
                    current.addProperty("isFavorite", rows.getInt("is_favorite") == 1);
                    current.add("clothesIds", new JsonArray());
                    outfits.add(current);
                    currentId = id;
                }
                int clothesId = rows.getInt("clothes_id");
                if (!rows.wasNull()) current.getAsJsonArray("clothesIds").add(clothesId);
            }
        }
        sendJson(exchange, 200, JSON.toJson(outfits));
    }

    private static void createOutfit(HttpExchange exchange) throws Exception {
        JsonObject input = readJson(exchange);
        String name = required(input, "name", 120);
        String importKey = input.has("importKey") ? required(input, "importKey", 120) : null;
        JsonArray ids = input.getAsJsonArray("clothesIds");
        if (ids == null || ids.isEmpty()) throw new IllegalArgumentException("Select clothes");
        try (Connection db = connect()) {
            db.setAutoCommit(false);
            try (PreparedStatement insert = db.prepareStatement(
                    "INSERT INTO coordination(name, is_favorite, import_key) VALUES (?, ?, ?)", new String[]{"ID"});
                 PreparedStatement link = db.prepareStatement(
                    "INSERT INTO coordination_item(coordination_id, clothes_id, display_order) VALUES (?, ?, ?)")) {
                Integer existingId = importedId(db, "coordination", importKey);
                if (existingId != null) {
                    sendJson(exchange, 200, "{\"id\":" + existingId + "}");
                    return;
                }
                insert.setString(1, name);
                insert.setInt(2, input.has("isFavorite") && input.get("isFavorite").getAsBoolean() ? 1 : 0);
                insert.setString(3, importKey);
                insert.executeUpdate();
                int outfitId;
                try (ResultSet keys = insert.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("No generated id");
                    outfitId = keys.getInt(1);
                }
                int order = 0;
                for (var value : ids) {
                    link.setInt(1, outfitId);
                    link.setInt(2, value.getAsInt());
                    link.setInt(3, order++);
                    link.executeUpdate();
                }
                db.commit();
                sendJson(exchange, 201, "{\"id\":" + outfitId + "}");
            } catch (Exception e) {
                db.rollback();
                throw e;
            }
        }
    }

    private static Integer importedId(Connection db, String table, String key) throws SQLException {
        if (key == null) return null;
        try (PreparedStatement sql = db.prepareStatement("SELECT id FROM " + table + " WHERE import_key = ?")) {
            sql.setString(1, key);
            try (ResultSet row = sql.executeQuery()) {
                return row.next() ? row.getInt(1) : null;
            }
        }
    }

    private static String required(JsonObject input, String key, int maxLength) {
        if (!input.has(key) || input.get(key).isJsonNull()) throw new IllegalArgumentException("Missing " + key);
        String value = input.get(key).getAsString().trim();
        if (value.isEmpty() || value.length() > maxLength) throw new IllegalArgumentException("Invalid " + key);
        return value;
    }

    private static void sendJson(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }
}
