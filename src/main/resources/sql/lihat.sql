-- =====================================================================
--  Student Enrollment Management System - LIHAT ISI DATABASE
--
--  Script baca-saja untuk memeriksa isi database dan mengambil
--  screenshot dokumentasi. TIDAK mengubah data apa pun -- hanya SELECT.
--
--  Jalankan:
--    sqlplus <user>/<password>@localhost:1521/XEPDB1 "@lihat.sql"
--
--  Lewat Docker:
--    docker cp src\main\resources\sql\lihat.sql sems-oracle:/tmp/lihat.sql
--    docker exec sems-oracle bash -c "cd /tmp && sqlplus -S <user>/<password>@localhost:1521/XEPDB1 @lihat.sql"
--
--  Isinya menjawab checklist screenshot README nomor 3, 5, dan 6.
-- =====================================================================

-- Lebar layar dilebarkan dan tiap kolom diberi lebar tetap. Tanpa ini
-- SQL*Plus memecah satu baris menjadi beberapa baris dan hasilnya sulit
-- dibaca -- terutama untuk kolom VARCHAR2(100) seperti nama mahasiswa.
SET LINESIZE 140
SET PAGESIZE 60
SET FEEDBACK ON
SET SQLBLANKLINES ON

PROMPT
PROMPT ============================================================
PROMPT  1. ISI TABEL MAHASISWA
PROMPT ============================================================
COLUMN nim      FORMAT A13    HEADING 'NIM'
COLUMN nama     FORMAT A26    HEADING 'NAMA'
COLUMN angkatan FORMAT 9999   HEADING 'ANGKATAN'
COLUMN gender   FORMAT A6     HEADING 'GENDER'
SELECT nim, nama, angkatan, gender
FROM mahasiswa
ORDER BY nim;

PROMPT
PROMPT ============================================================
PROMPT  2. ISI TABEL MATA_KULIAH
PROMPT ============================================================
COLUMN matkul_id   FORMAT A10  HEADING 'KODE'
COLUMN matkul_nama FORMAT A34  HEADING 'NAMA MATA KULIAH'
COLUMN sks         FORMAT 999  HEADING 'SKS'
COLUMN hari        FORMAT A8   HEADING 'HARI'
SELECT matkul_id, matkul_nama, sks, hari
FROM mata_kuliah
ORDER BY matkul_id;

PROMPT
PROMPT ============================================================
PROMPT  3. ISI TABEL IRS
PROMPT ============================================================
COLUMN irs_id    FORMAT 99999  HEADING 'IRS_ID'
COLUMN nim       FORMAT A13    HEADING 'NIM'
COLUMN matkul_id FORMAT A10    HEADING 'KODE MK'
COLUMN status    FORMAT A8     HEADING 'STATUS'
SELECT irs_id, nim, matkul_id, status
FROM irs
ORDER BY irs_id;

PROMPT
PROMPT ============================================================
PROMPT  4. HASIL QUERY JOIN - sama persis dengan yang dipakai
PROMPT     halaman detail aplikasi (/students/{nim})
PROMPT ============================================================
COLUMN nim         FORMAT A13  HEADING 'NIM'
COLUMN nama        FORMAT A20  HEADING 'MAHASISWA'
COLUMN matkul_nama FORMAT A32  HEADING 'MATA KULIAH'
COLUMN sks         FORMAT 999  HEADING 'SKS'
COLUMN hari        FORMAT A8   HEADING 'HARI'
COLUMN status      FORMAT A8   HEADING 'STATUS'
SELECT m.nim, m.nama, mk.matkul_nama, mk.sks, mk.hari, i.status
FROM mahasiswa m
LEFT JOIN irs i          ON i.nim = m.nim
LEFT JOIN mata_kuliah mk ON mk.matkul_id = i.matkul_id
ORDER BY m.nim, mk.matkul_id NULLS LAST;

PROMPT
PROMPT  Catatan: Dian Kusuma (24060123002) muncul dengan kolom mata
PROMPT  kuliah KOSONG. Itu memang benar -- dia sengaja tidak diberi
PROMPT  baris IRS, dan LEFT JOIN membuatnya tetap terbawa. Dengan
PROMPT  INNER JOIN, baris itu akan hilang sama sekali.

PROMPT
PROMPT ============================================================
PROMPT  5. JUMLAH BARIS PER TABEL (harusnya 4 / 4 / 7)
PROMPT ============================================================
COLUMN tabel  FORMAT A14  HEADING 'TABEL'
COLUMN jumlah FORMAT 9999 HEADING 'JUMLAH'
SELECT 'mahasiswa'   AS tabel, COUNT(*) AS jumlah FROM mahasiswa
UNION ALL
SELECT 'mata_kuliah',          COUNT(*)           FROM mata_kuliah
UNION ALL
SELECT 'irs',                  COUNT(*)           FROM irs;

PROMPT
PROMPT ============================================================
PROMPT  6. STRUKTUR TABEL  (checklist screenshot README no. 5)
PROMPT ============================================================
DESC mahasiswa
DESC mata_kuliah
DESC irs

PROMPT
PROMPT ============================================================
PROMPT  7. DAFTAR CONSTRAINT  (checklist screenshot README no. 6)
PROMPT
PROMPT  Membuktikan kelima jenis constraint materi SBD terpasang:
PROMPT  NOT NULL, PRIMARY KEY, FOREIGN KEY, UNIQUE, dan CHECK.
PROMPT ============================================================
COLUMN tabel     FORMAT A13  HEADING 'TABEL'
COLUMN nama      FORMAT A24  HEADING 'NAMA CONSTRAINT'
COLUMN tipe      FORMAT A13  HEADING 'TIPE'
COLUMN on_delete FORMAT A10  HEADING 'ON DELETE'
SELECT table_name AS tabel,
       constraint_name AS nama,
       CASE constraint_type
            WHEN 'P' THEN 'PRIMARY KEY'
            WHEN 'R' THEN 'FOREIGN KEY'
            WHEN 'U' THEN 'UNIQUE'
            WHEN 'C' THEN 'CHECK'
       END AS tipe,
       NVL(delete_rule, '-') AS on_delete
FROM user_constraints
WHERE table_name IN ('MAHASISWA', 'MATA_KULIAH', 'IRS')
  AND constraint_name NOT LIKE 'SYS\_C%' ESCAPE '\'
ORDER BY table_name, tipe, constraint_name;

PROMPT
PROMPT  Perhatikan kolom ON DELETE:
PROMPT    FK_IRS_MAHASISWA   = CASCADE    -> hapus mahasiswa ikut hapus IRS-nya
PROMPT    FK_IRS_MATA_KULIAH = NO ACTION  -> mata kuliah yang masih diambil
PROMPT                                      ditolak penghapusannya
PROMPT
PROMPT  Constraint bernama SYS_C... sengaja disembunyikan; itu NOT NULL
PROMPT  yang dibuat Oracle otomatis dan namanya berubah-ubah.

PROMPT
PROMPT ============================================================
PROMPT  Selesai. Tidak ada data yang diubah oleh script ini.
PROMPT ============================================================

EXIT;
