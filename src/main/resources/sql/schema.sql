-- =====================================================================
--  Student Enrollment Management System - SCHEMA
--  Target  : Oracle Database (XE 21c / 18c)
--  Jalankan : sqlplus <user>/<password>@localhost:1521/XEPDB1 @schema.sql
--
--  Script ini aman dijalankan berulang kali: tabel lama dihapus dulu
--  bila ada. Oracle tidak mendukung "DROP TABLE IF EXISTS", jadi
--  penghapusan dibungkus blok PL/SQL yang mengabaikan error
--  ORA-00942 (table or view does not exist).
-- =====================================================================

SET SERVEROUTPUT ON

-- SQLBLANKLINES ON wajib ada. Secara default SQL*Plus memperlakukan baris
-- kosong sebagai AKHIR statement, sehingga CREATE TABLE di bawah -- yang
-- punya baris kosong sebelum blok CONSTRAINT agar mudah dibaca -- akan
-- terpotong dan gagal dengan "SP2-0734: unknown command beginning
-- CONSTRAINT...". Tanpa baris ini, ketiga tabel tidak pernah terbentuk.
SET SQLBLANKLINES ON

-- Berhenti begitu ada error, jangan diteruskan sampai mencetak pesan
-- "Schema berhasil dibuat" yang menyesatkan.
WHENEVER SQLERROR EXIT SQL.SQLCODE

-- --------------------------------------------------------------------
-- 0. Bersihkan tabel lama (urutan terbalik dari dependensi FK)
-- --------------------------------------------------------------------
BEGIN
    FOR t IN (SELECT 'IRS' AS nama FROM dual
              UNION ALL SELECT 'MATA_KULIAH' FROM dual
              UNION ALL SELECT 'MAHASISWA'   FROM dual)
    LOOP
        BEGIN
            EXECUTE IMMEDIATE 'DROP TABLE ' || t.nama || ' CASCADE CONSTRAINTS';
            DBMS_OUTPUT.PUT_LINE('Tabel ' || t.nama || ' dihapus.');
        EXCEPTION
            WHEN OTHERS THEN
                IF SQLCODE != -942 THEN   -- -942 = tabel memang belum ada
                    RAISE;
                END IF;
        END;
    END LOOP;
END;
/

-- --------------------------------------------------------------------
-- 1. MAHASISWA
-- --------------------------------------------------------------------
CREATE TABLE mahasiswa (
    nim       VARCHAR2(20)  NOT NULL,
    nama      VARCHAR2(100) NOT NULL,
    angkatan  NUMBER(4)     NOT NULL,
    gender    CHAR(1)       NOT NULL,

    CONSTRAINT pk_mahasiswa           PRIMARY KEY (nim),
    CONSTRAINT ck_mahasiswa_gender    CHECK (gender IN ('L', 'P')),
    CONSTRAINT ck_mahasiswa_angkatan  CHECK (angkatan BETWEEN 2000 AND 2100),
    CONSTRAINT ck_mahasiswa_nama      CHECK (TRIM(nama) IS NOT NULL)
);

COMMENT ON TABLE  mahasiswa          IS 'Data induk mahasiswa';
COMMENT ON COLUMN mahasiswa.nim      IS 'Nomor Induk Mahasiswa, primary key';
COMMENT ON COLUMN mahasiswa.gender   IS 'L = laki-laki, P = perempuan';

-- --------------------------------------------------------------------
-- 2. MATA_KULIAH
-- --------------------------------------------------------------------
CREATE TABLE mata_kuliah (
    matkul_id    VARCHAR2(10)  NOT NULL,
    matkul_nama  VARCHAR2(100) NOT NULL,
    sks          NUMBER(2)     NOT NULL,
    hari         VARCHAR2(10)  NOT NULL,

    CONSTRAINT pk_mata_kuliah       PRIMARY KEY (matkul_id),
    CONSTRAINT ck_mata_kuliah_sks   CHECK (sks BETWEEN 1 AND 6),
    CONSTRAINT ck_mata_kuliah_hari  CHECK (hari IN ('Senin','Selasa','Rabu','Kamis','Jumat','Sabtu'))
);

COMMENT ON TABLE  mata_kuliah      IS 'Katalog mata kuliah yang ditawarkan';
COMMENT ON COLUMN mata_kuliah.sks  IS 'Satuan Kredit Semester, 1..6';

-- --------------------------------------------------------------------
-- 3. IRS (Isian Rencana Studi) - tabel relasi mahasiswa x mata kuliah
-- --------------------------------------------------------------------
CREATE TABLE irs (
    irs_id     NUMBER(10)   NOT NULL,
    nim        VARCHAR2(20) NOT NULL,
    matkul_id  VARCHAR2(10) NOT NULL,
    status     VARCHAR2(10) NOT NULL,

    CONSTRAINT pk_irs        PRIMARY KEY (irs_id),

    -- Menghapus mahasiswa ikut menghapus baris IRS-nya, sehingga fitur
    -- Hapus di aplikasi tidak gagal dengan ORA-02292.
    CONSTRAINT fk_irs_mahasiswa   FOREIGN KEY (nim)
        REFERENCES mahasiswa (nim) ON DELETE CASCADE,

    -- Mata kuliah TIDAK cascade: mata kuliah yang masih diambil orang
    -- memang harus ditolak penghapusannya.
    CONSTRAINT fk_irs_mata_kuliah FOREIGN KEY (matkul_id)
        REFERENCES mata_kuliah (matkul_id),

    -- Satu mahasiswa tidak boleh mengambil mata kuliah yang sama dua kali.
    CONSTRAINT uq_irs_nim_matkul  UNIQUE (nim, matkul_id),

    CONSTRAINT ck_irs_status      CHECK (status IN ('aktif', 'lulus', 'gagal'))
);

COMMENT ON TABLE  irs         IS 'Isian Rencana Studi: mata kuliah yang diambil mahasiswa';
COMMENT ON COLUMN irs.status  IS 'aktif = sedang berjalan, lulus = selesai lulus, gagal = tidak lulus';

-- --------------------------------------------------------------------
-- 4. Index pendukung
--    Kolom nim di IRS dipakai pada JOIN halaman detail mahasiswa.
--    Constraint uq_irs_nim_matkul sudah membuat index gabungan
--    (nim, matkul_id) yang bisa dipakai sebagai leading-column index
--    untuk nim, jadi TIDAK perlu index tambahan untuk kolom itu.
--    Index di bawah untuk kolom matkul_id (kolom kedua pada unique index,
--    sehingga tidak tercakup).
-- --------------------------------------------------------------------
CREATE INDEX ix_irs_matkul_id ON irs (matkul_id);

-- --------------------------------------------------------------------
-- 5. Sequence untuk irs_id
--    Belum dipakai aplikasi (IRS pada versi ini diisi lewat seed.sql),
--    tetapi disediakan agar penambahan IRS manual tetap rapi:
--        INSERT INTO irs VALUES (seq_irs.NEXTVAL, '24060121001', 'IF2101', 'aktif');
-- --------------------------------------------------------------------
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE seq_irs';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -2289 THEN   -- -2289 = sequence memang belum ada
            RAISE;
        END IF;
END;
/

CREATE SEQUENCE seq_irs START WITH 1001 INCREMENT BY 1 NOCACHE;

COMMIT;

PROMPT
PROMPT ============================================
PROMPT  Schema berhasil dibuat.
PROMPT  Lanjutkan dengan: @seed.sql
PROMPT ============================================
