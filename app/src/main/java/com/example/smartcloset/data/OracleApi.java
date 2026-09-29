package com.example.smartcloset.data;

import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class OracleApi {
    public static final String BASE_URL = "http://10.0.2.2:8080";

    private OracleApi() {
    }

    public static List<ClothesEntity> getClothes() throws Exception {
        JSONArray response = new JSONArray(request("GET", "/clothes", null));
        List<ClothesEntity> items = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            JSONObject row = response.getJSONObject(i);
            int id = row.getInt("id");
            ClothesEntity item = new ClothesEntity(
                    BASE_URL + "/clothes/" + id + "/image",
                    row.getString("category"),
                    row.getString("color"),
                    row.getString("season"),
                    row.getBoolean("isFavorite")
            );
            item.id = id;
            items.add(item);
        }
        return items;
    }

    public static int createClothes(ClothesEntity item) throws Exception {
        return createClothes(item, null);
    }

    public static int createClothes(ClothesEntity item, String importKey) throws Exception {
        File image = new File(item.imagePath);
        if (image.length() > 6 * 1024 * 1024) {
            throw new IOException("Image exceeds 6 MB");
        }
        byte[] bytes = new byte[(int) image.length()];
        try (InputStream stream = new FileInputStream(image)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = stream.read(bytes, offset, bytes.length - offset);
                if (read < 0) throw new IOException("Incomplete image");
                offset += read;
            }
        }
        JSONObject body = new JSONObject();
        body.put("category", item.category);
        body.put("color", item.color);
        body.put("season", item.season);
        body.put("isFavorite", item.isFavorite);
        body.put("imageMime", imageMime(bytes));
        body.put("imageBase64", Base64.encodeToString(bytes, Base64.NO_WRAP));
        if (importKey != null) body.put("importKey", importKey);
        return new JSONObject(request("POST", "/clothes", body)).getInt("id");
    }

    private static String imageMime(byte[] image) {
        if (image.length >= 8 && image[0] == (byte) 0x89 && image[1] == 'P'
                && image[2] == 'N' && image[3] == 'G') return "image/png";
        if (image.length >= 12 && image[0] == 'R' && image[1] == 'I'
                && image[8] == 'W' && image[9] == 'E' && image[10] == 'B' && image[11] == 'P') return "image/webp";
        if (image.length >= 3 && image[0] == (byte) 0xFF
                && image[1] == (byte) 0xD8 && image[2] == (byte) 0xFF) return "image/jpeg";
        throw new IllegalArgumentException("Select a JPEG, PNG, or WebP image");
    }

    public static void deleteClothes(int id) throws Exception {
        request("DELETE", "/clothes/" + id, null);
    }

    public static void setClothesFavorite(int id, boolean favorite) throws Exception {
        JSONObject body = new JSONObject();
        body.put("isFavorite", favorite);
        request("PUT", "/clothes/" + id + "/favorite", body);
    }

    public static List<CoordinationEntity> getOutfits() throws Exception {
        JSONArray response = new JSONArray(request("GET", "/outfits", null));
        List<CoordinationEntity> items = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            JSONObject row = response.getJSONObject(i);
            JSONArray ids = row.getJSONArray("clothesIds");
            StringBuilder joined = new StringBuilder();
            for (int j = 0; j < ids.length(); j++) {
                if (j > 0) joined.append(',');
                joined.append(ids.getInt(j));
            }
            int firstId = ids.length() > 0 ? ids.getInt(0) : 0;
            int secondId = ids.length() > 1 ? ids.getInt(1) : firstId;
            CoordinationEntity item = new CoordinationEntity(
                    row.getString("name"), firstId, secondId,
                    row.getBoolean("isFavorite"), joined.toString()
            );
            item.coordiId = row.getInt("id");
            items.add(item);
        }
        return items;
    }

    public static int createOutfit(CoordinationEntity item) throws Exception {
        return createOutfit(item, null);
    }

    public static int createOutfit(CoordinationEntity item, String importKey) throws Exception {
        JSONObject body = new JSONObject();
        body.put("name", item.coordiName);
        body.put("isFavorite", item.isFavorite);
        JSONArray ids = new JSONArray();
        for (String part : item.clothesIds.split(",")) ids.put(Integer.parseInt(part.trim()));
        body.put("clothesIds", ids);
        if (importKey != null) body.put("importKey", importKey);
        return new JSONObject(request("POST", "/outfits", body)).getInt("id");
    }

    public static void setOutfitFavorite(int id, boolean favorite) throws Exception {
        JSONObject body = new JSONObject();
        body.put("isFavorite", favorite);
        request("PUT", "/outfits/" + id + "/favorite", body);
    }

    private static String request(String method, String path, JSONObject body) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(BASE_URL + path).openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(10000);
        try {
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                try (java.io.OutputStream output = connection.getOutputStream()) {
                    output.write(bytes);
                }
            }
            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String result = stream == null ? "" : new String(readFully(stream), StandardCharsets.UTF_8);
            if (status >= 400) throw new IOException("Oracle API HTTP " + status + ": " + result);
            return result;
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readFully(InputStream stream) throws IOException {
        try (InputStream input = stream; java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            return output.toByteArray();
        }
    }
}
