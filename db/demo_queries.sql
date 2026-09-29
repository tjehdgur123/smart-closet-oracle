SET LINESIZE 180
SET PAGESIZE 60
COLUMN category FORMAT A16
COLUMN color FORMAT A16
COLUMN season FORMAT A16
COLUMN name FORMAT A30

PROMPT 1. Registered clothes by category/color/season
SELECT id, category, color, season, is_favorite, DBMS_LOB.GETLENGTH(image_data) AS image_bytes
FROM clothes ORDER BY id;

PROMPT 2. Outfits with clothes count
SELECT c.id, c.name, c.is_favorite, COUNT(i.clothes_id) AS clothes_count
FROM coordination c LEFT JOIN coordination_item i ON i.coordination_id = c.id
GROUP BY c.id, c.name, c.is_favorite ORDER BY c.id;

PROMPT 3. Outfit-clothes relationship
SELECT c.name, i.display_order, cl.id AS clothes_id, cl.category, cl.color, cl.season
FROM coordination c
JOIN coordination_item i ON i.coordination_id = c.id
JOIN clothes cl ON cl.id = i.clothes_id
ORDER BY c.id, i.display_order;

PROMPT 4. Favorite clothes
SELECT id, category, color, season FROM clothes WHERE is_favorite = 1 ORDER BY id;
EXIT
