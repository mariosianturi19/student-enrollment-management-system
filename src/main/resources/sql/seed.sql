-- =====================================================================
--  Student Enrollment Management System - SEED DATA
--  Target  : Oracle Database (XE 21c / 18c)
--  Jalankan: sqlplus <user>/<password>@localhost:1521/XEPDB1 @seed.sql
--
--  PRASYARAT: schema.sql sudah dijalankan lebih dulu.
--
--  Semua data di bawah adalah data contoh fiktif. Nama, NIM, dan kode
--  mata kuliah dikarang bebas dan boleh diubah sesuka hati -- tidak ada
--  kaitannya dengan data mahasiswa sungguhan mana pun.
-- =====================================================================

-- Lihat catatan di schema.sql: tanpa SQLBLANKLINES ON, baris kosong di
-- tengah statement memotongnya. Disetel juga di sini agar seed.sql tetap
-- benar bila dijalankan sendirian di sesi sqlplus yang baru.
SET SQLBLANKLINES ON

-- Gagal seketika bila ada error, supaya pesan "Seed data berhasil dimuat"
-- di akhir tidak pernah tercetak untuk seed yang sebenarnya gagal.
WHENEVER SQLERROR EXIT SQL.SQLCODE

-- Kosongkan isi tabel lebih dulu supaya seed bisa dijalankan berulang
-- tanpa melanggar primary key. Urutan mengikuti dependensi FK.
DELETE FROM irs;
DELETE FROM mahasiswa;
DELETE FROM mata_kuliah;

-- --------------------------------------------------------------------
-- MAHASISWA (4 baris)
-- Catatan: 'Dian Kusuma' sengaja TIDAK diberi baris IRS sama sekali,
-- supaya empty state pada halaman detail bisa diuji dan di-screenshot.
-- --------------------------------------------------------------------
INSERT INTO mahasiswa (nim, nama, angkatan, gender) VALUES ('24060122001', 'Sekar Ayu Pramesti',   2022, 'P');
INSERT INTO mahasiswa (nim, nama, angkatan, gender) VALUES ('24060122002', 'Bagas Prasetyo',       2022, 'L');
INSERT INTO mahasiswa (nim, nama, angkatan, gender) VALUES ('24060123001', 'Rizky Hidayat',        2023, 'L');
INSERT INTO mahasiswa (nim, nama, angkatan, gender) VALUES ('24060123002', 'Dian Kusuma',          2023, 'P');

-- --------------------------------------------------------------------
-- MATA_KULIAH (4 baris)
-- --------------------------------------------------------------------
INSERT INTO mata_kuliah (matkul_id, matkul_nama, sks, hari) VALUES ('IF2101', 'Sistem Basis Data',        3, 'Senin');
INSERT INTO mata_kuliah (matkul_id, matkul_nama, sks, hari) VALUES ('IF2102', 'Pemrograman Berorientasi Objek', 3, 'Selasa');
INSERT INTO mata_kuliah (matkul_id, matkul_nama, sks, hari) VALUES ('IF2103', 'Jaringan Komputer',        2, 'Rabu');
INSERT INTO mata_kuliah (matkul_id, matkul_nama, sks, hari) VALUES ('IF2104', 'Struktur Data',            4, 'Kamis');

-- --------------------------------------------------------------------
-- IRS (7 baris)
-- Sengaja mencampur ketiga status supaya ketiga varian badge terlihat.
-- --------------------------------------------------------------------

-- Sekar Ayu Pramesti - 3 mata kuliah, semua status berbeda
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1001, '24060122001', 'IF2101', 'aktif');
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1002, '24060122001', 'IF2102', 'lulus');
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1003, '24060122001', 'IF2104', 'gagal');

-- Bagas Prasetyo - 2 mata kuliah
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1004, '24060122002', 'IF2101', 'lulus');
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1005, '24060122002', 'IF2103', 'aktif');

-- Rizky Hidayat - 2 mata kuliah
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1006, '24060123001', 'IF2102', 'aktif');
INSERT INTO irs (irs_id, nim, matkul_id, status) VALUES (1007, '24060123001', 'IF2104', 'aktif');

-- Dian Kusuma - sengaja kosong, untuk menguji empty state.

COMMIT;

-- Selaraskan sequence agar nilai berikutnya tidak bentrok dengan seed.
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE seq_irs';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2289 THEN
            RAISE;
        END IF;
END;
/

CREATE SEQUENCE seq_irs START WITH 1008 INCREMENT BY 1 NOCACHE;

-- --------------------------------------------------------------------
-- Verifikasi cepat
-- --------------------------------------------------------------------
PROMPT
PROMPT === Jumlah baris per tabel (harusnya 4 / 4 / 7) ===
SELECT 'mahasiswa'   AS tabel, COUNT(*) AS jumlah FROM mahasiswa
UNION ALL
SELECT 'mata_kuliah',          COUNT(*)           FROM mata_kuliah
UNION ALL
SELECT 'irs',                  COUNT(*)           FROM irs;

PROMPT
PROMPT === Contoh hasil JOIN untuk NIM 24060122001 (harusnya 3 baris) ===
SELECT m.nim, m.nama, mk.matkul_nama, mk.sks, mk.hari, i.status
FROM mahasiswa m
LEFT JOIN irs i          ON i.nim = m.nim
LEFT JOIN mata_kuliah mk ON mk.matkul_id = i.matkul_id
WHERE m.nim = '24060122001'
ORDER BY mk.matkul_id;

PROMPT
PROMPT ============================================
PROMPT  Seed data berhasil dimuat.
PROMPT ============================================
