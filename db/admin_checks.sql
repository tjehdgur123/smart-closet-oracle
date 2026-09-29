SET LINESIZE 160
SET PAGESIZE 60
COLUMN name FORMAT A25
COLUMN open_mode FORMAT A20
COLUMN username FORMAT A22
COLUMN status FORMAT A12

PROMPT Oracle PDB availability
SELECT name, open_mode FROM v$pdbs ORDER BY name;

PROMPT Smart Closet database sessions
SELECT username, status, COUNT(*) AS sessions
FROM v$session
WHERE username = 'SMART_CLOSET'
GROUP BY username, status;
EXIT
