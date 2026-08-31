package com.togar.studentenrollment.repository;

import com.togar.studentenrollment.dto.EnrolledCourse;
import com.togar.studentenrollment.dto.MahasiswaDetail;
import com.togar.studentenrollment.model.Mahasiswa;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seluruh akses SQL ke tabel MAHASISWA (dan JOIN-nya ke IRS / MATA_KULIAH).
 *
 * <p><strong>Keamanan:</strong> setiap nilai dari luar dikirim sebagai parameter
 * {@code ?} ke {@link JdbcTemplate}, yang menerjemahkannya ke
 * {@code PreparedStatement}. Tidak ada string concatenation untuk membentuk SQL
 * di kelas ini, sehingga tidak ada permukaan SQL injection.
 */
@Repository
public class MahasiswaRepository {

    private static final String SQL_FIND_ALL = """
            SELECT nim, nama, angkatan, gender
            FROM mahasiswa
            ORDER BY angkatan, nama
            """;

    private static final String SQL_FIND_BY_NIM = """
            SELECT nim, nama, angkatan, gender
            FROM mahasiswa
            WHERE nim = ?
            """;

    private static final String SQL_EXISTS = """
            SELECT COUNT(*)
            FROM mahasiswa
            WHERE nim = ?
            """;

    private static final String SQL_INSERT = """
            INSERT INTO mahasiswa (nim, nama, angkatan, gender)
            VALUES (?, ?, ?, ?)
            """;

    private static final String SQL_UPDATE = """
            UPDATE mahasiswa
            SET nim = ?, nama = ?, angkatan = ?, gender = ?
            WHERE nim = ?
            """;

    private static final String SQL_DELETE = """
            DELETE FROM mahasiswa
            WHERE nim = ?
            """;

    /**
     * Satu query JOIN untuk halaman detail.
     *
     * <p>Memakai {@code LEFT JOIN} dua tingkat supaya mahasiswa yang belum
     * mengambil mata kuliah apa pun tetap terbawa (satu baris dengan kolom
     * IRS/MATA_KULIAH bernilai NULL). Kalau memakai {@code INNER JOIN},
     * mahasiswa tanpa IRS akan hilang sama sekali dan halaman detailnya
     * salah menampilkan 404.
     *
     * <p>{@code ORDER BY mk.matkul_id NULLS LAST} — sintaks Oracle — menjaga
     * baris NULL (kasus tanpa IRS) tetap di posisi yang dapat diprediksi.
     */
    private static final String SQL_FIND_DETAIL = """
            SELECT m.nim,
                   m.nama,
                   m.angkatan,
                   m.gender,
                   mk.matkul_id,
                   mk.matkul_nama,
                   mk.sks,
                   mk.hari,
                   i.irs_id,
                   i.status
            FROM mahasiswa m
            LEFT JOIN irs i          ON i.nim = m.nim
            LEFT JOIN mata_kuliah mk ON mk.matkul_id = i.matkul_id
            WHERE m.nim = ?
            ORDER BY mk.matkul_id NULLS LAST
            """;

    private static final RowMapper<Mahasiswa> MAHASISWA_MAPPER = (rs, rowNum) -> new Mahasiswa(
            rs.getString("nim"),
            rs.getString("nama"),
            rs.getInt("angkatan"),
            rs.getString("gender"));

    private final JdbcTemplate jdbcTemplate;

    public MahasiswaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Mahasiswa> findAll() {
        return jdbcTemplate.query(SQL_FIND_ALL, MAHASISWA_MAPPER);
    }

    public Optional<Mahasiswa> findByNim(String nim) {
        List<Mahasiswa> found = jdbcTemplate.query(SQL_FIND_BY_NIM, MAHASISWA_MAPPER, nim);
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    public boolean existsByNim(String nim) {
        Integer count = jdbcTemplate.queryForObject(SQL_EXISTS, Integer.class, nim);
        return count != null && count > 0;
    }

    /**
     * Mengambil mahasiswa beserta mata kuliahnya dengan <strong>satu</strong>
     * perjalanan ke database.
     *
     * <p>{@code ResultSetExtractor} dipakai (bukan {@code RowMapper}) karena
     * satu objek {@link MahasiswaDetail} dirakit dari banyak baris ResultSet.
     *
     * @return {@link Optional#empty()} bila NIM tidak ada di tabel MAHASISWA
     */
    public Optional<MahasiswaDetail> findDetailByNim(String nim) {
        MahasiswaDetail detail = jdbcTemplate.query(SQL_FIND_DETAIL, rs -> {
            Mahasiswa mahasiswa = null;
            List<EnrolledCourse> courses = new ArrayList<>();

            while (rs.next()) {
                if (mahasiswa == null) {
                    mahasiswa = new Mahasiswa(
                            rs.getString("nim"),
                            rs.getString("nama"),
                            rs.getInt("angkatan"),
                            rs.getString("gender"));
                }
                EnrolledCourse course = readCourse(rs);
                if (course != null) {
                    courses.add(course);
                }
            }

            // ResultSet kosong berarti NIM tidak ada. Karena LEFT JOIN, mahasiswa
            // yang ada tetapi tanpa IRS menghasilkan satu baris, bukan nol baris.
            return mahasiswa == null ? null : new MahasiswaDetail(mahasiswa, courses);
        }, nim);

        return Optional.ofNullable(detail);
    }

    /**
     * Membaca kolom hasil JOIN menjadi satu {@link EnrolledCourse}.
     *
     * @return {@code null} bila baris ini berasal dari mahasiswa tanpa IRS
     *         (seluruh kolom IRS/MATA_KULIAH bernilai NULL)
     */
    private EnrolledCourse readCourse(ResultSet rs) throws SQLException {
        String matkulId = rs.getString("matkul_id");
        if (matkulId == null) {
            return null;
        }
        return new EnrolledCourse(
                rs.getLong("irs_id"),
                matkulId,
                rs.getString("matkul_nama"),
                rs.getInt("sks"),
                rs.getString("hari"),
                rs.getString("status"));
    }

    public void insert(Mahasiswa m) {
        jdbcTemplate.update(SQL_INSERT, m.nim(), m.nama(), m.angkatan(), m.gender());
    }

    /**
     * @param originalNim NIM sebelum diubah; dipakai pada klausa WHERE sehingga
     *                    NIM (primary key) juga bisa diperbarui
     * @return jumlah baris terpengaruh; 0 berarti NIM lama sudah tidak ada
     */
    public int update(String originalNim, Mahasiswa m) {
        return jdbcTemplate.update(SQL_UPDATE,
                m.nim(), m.nama(), m.angkatan(), m.gender(), originalNim);
    }

    /** @return jumlah baris terhapus; 0 berarti NIM tidak ditemukan */
    public int deleteByNim(String nim) {
        return jdbcTemplate.update(SQL_DELETE, nim);
    }
}
