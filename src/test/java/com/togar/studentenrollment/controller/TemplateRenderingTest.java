package com.togar.studentenrollment.controller;

import com.togar.studentenrollment.config.WebBindingConfig;
import com.togar.studentenrollment.dto.EnrolledCourse;
import com.togar.studentenrollment.dto.MahasiswaDetail;
import com.togar.studentenrollment.exception.MahasiswaNotFoundException;
import com.togar.studentenrollment.model.Mahasiswa;
import com.togar.studentenrollment.service.MahasiswaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Membuktikan bahwa template Thymeleaf benar-benar <em>dirender</em> menjadi HTML,
 * bukan sekadar dicocokkan nama view-nya.
 *
 * <p>Tanpa kelas ini, kesalahan sintaks Thymeleaf (fragment salah alamat, ekspresi
 * yang tidak dikenal, atribut yang tidak valid) tidak akan pernah ketahuan oleh
 * test lain — MockMvc akan tetap lulus selama nama view-nya cocok. Di sini isi
 * HTML-nya diperiksa, sehingga kegagalan render langsung menjatuhkan test.
 *
 * <p>Tetap tanpa database: {@code @WebMvcTest} tidak membuat {@code DataSource}.
 */
@WebMvcTest(MahasiswaController.class)
@Import({WebBindingConfig.class, GlobalExceptionHandler.class})
class TemplateRenderingTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MahasiswaService service;

    private static final Mahasiswa SEKAR =
            new Mahasiswa("24060122001", "Sekar Ayu Pramesti", 2022, "P");

    // ------------------------------------------------------ halaman daftar

    @Test
    @DisplayName("Halaman daftar merender baris data, layout, dan tombol aksi")
    void listPage_renderIsiTabel() throws Exception {
        given(service.findAll()).willReturn(List.of(SEKAR));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Daftar Mahasiswa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("24060122001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sekar Ayu Pramesti")))
                // genderLabel() dipanggil dari template
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Perempuan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1 mahasiswa terdaftar")))
                // layout ikut terpasang
                .andExpect(content().string(org.hamcrest.Matchers.containsString("SEMS")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/css/app.css")))
                // hapus berupa form POST, bukan tautan GET
                .andExpect(content().string(org.hamcrest.Matchers.containsString("method=\"post\"")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("/students/24060122001/delete")));
    }

    @Test
    @DisplayName("Halaman daftar tanpa data merender empty state, bukan tabel kosong")
    void listPage_kosong_renderEmptyState() throws Exception {
        given(service.findAll()).willReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Belum ada mahasiswa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("seed.sql")))
                // tabelnya sendiri tidak dirender sama sekali.
                // Dicek lewat tag "<table", bukan nama kelas CSS, karena nama kelas
                // juga muncul di komentar HTML yang menjelaskan perilaku responsif.
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<table"))));
    }

    @Test
    @DisplayName("Halaman dikirim sebagai UTF-8 sehingga karakter non-ASCII tidak rusak")
    void halaman_dikirimSebagaiUtf8() throws Exception {
        given(service.findAll()).willReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().encoding("UTF-8"))
                // em dash pada judul halaman dan footer harus utuh, bukan "â€”"
                .andExpect(content().string(org.hamcrest.Matchers.containsString(
                        "Daftar Mahasiswa — Student Enrollment Management System")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("â€”"))));
    }

    // ------------------------------------------------------ halaman detail

    @Test
    @DisplayName("Halaman detail merender biodata, tabel JOIN, badge status, dan total SKS")
    void detailPage_renderTabelJoin() throws Exception {
        given(service.findDetail("24060122001")).willReturn(new MahasiswaDetail(SEKAR, List.of(
                new EnrolledCourse(1001, "IF2101", "Sistem Basis Data", 3, "Senin", "aktif"),
                new EnrolledCourse(1002, "IF2102", "Pemrograman Berorientasi Objek", 3, "Selasa", "lulus"),
                new EnrolledCourse(1003, "IF2104", "Struktur Data", 4, "Kamis", "gagal"))));

        mvc.perform(get("/students/24060122001"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Mata Kuliah yang Diambil")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Sistem Basis Data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("IF2101")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Struktur Data")))
                // totalSks() = 3 + 3 + 4
                .andExpect(content().string(org.hamcrest.Matchers.containsString("10 SKS")))
                // ketiga varian badge dari statusBadgeClass()
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-aktif")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-lulus")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("badge-gagal")))
                // tombol kembali ke daftar
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kembali ke Daftar")));
    }

    @Test
    @DisplayName("Halaman detail tanpa mata kuliah merender empty state khusus")
    void detailPage_tanpaMataKuliah_renderEmptyState() throws Exception {
        given(service.findDetail("24060123002")).willReturn(new MahasiswaDetail(
                new Mahasiswa("24060123002", "Dian Kusuma", 2023, "P"), List.of()));

        mvc.perform(get("/students/24060123002"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Belum ada mata kuliah")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Dian Kusuma")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Kembali ke Daftar")));
    }

    // -------------------------------------------------------- halaman form

    @Test
    @DisplayName("Form tambah merender keempat field beserta label yang terhubung")
    void formTambah_renderSeluruhField() throws Exception {
        mvc.perform(get("/students/new"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tambah Mahasiswa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"nim\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"nama\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"angkatan\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("for=\"gender\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"nim\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Laki-laki")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Perempuan")))
                // action mengarah ke endpoint tambah, bukan edit
                .andExpect(content().string(org.hamcrest.Matchers.containsString("action=\"/students\"")));
    }

    @Test
    @DisplayName("Form edit merender nilai lama dan action ke endpoint edit")
    void formEdit_renderNilaiLama() throws Exception {
        given(service.findByNim("24060122001")).willReturn(SEKAR);

        mvc.perform(get("/students/24060122001/edit"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Ubah Data Mahasiswa")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"24060122001\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"Sekar Ayu Pramesti\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Simpan Perubahan")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("action=\"/students/24060122001/edit\"")));
    }

    @Test
    @DisplayName("Form yang gagal validasi merender pesan error dan mempertahankan isian")
    void form_gagalValidasi_renderPesanErrorDanIsian() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "24060122ABC")
                        .param("nama", "")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                // pesan dari anotasi @Pattern dan @NotBlank benar-benar tampil di HTML
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("NIM harus berupa 8 sampai 20 digit angka")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Nama wajib diisi")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("is-invalid")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Data belum bisa disimpan")))
                // isian yang sudah diketik tidak hilang
                .andExpect(content().string(org.hamcrest.Matchers.containsString("value=\"24060122ABC\"")));
    }

    // ------------------------------------------------------- halaman error

    @Test
    @DisplayName("Halaman 404 merender NIM yang dicari dan tautan kembali")
    void halaman404_render() throws Exception {
        given(service.findDetail("99999999"))
                .willThrow(new MahasiswaNotFoundException("99999999"));

        mvc.perform(get("/students/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Mahasiswa tidak ditemukan")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("99999999")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Kembali ke Daftar Mahasiswa")));
    }

    /**
     * Regresi: saat Oracle mati, {@code @Transactional} pada service gagal
     * membuka koneksi dan melempar {@link org.springframework.transaction.CannotCreateTransactionException},
     * yang <em>bukan</em> turunan {@code DataAccessException}. Sebelum diperbaiki,
     * kasus ini lolos dari handler dan pengguna menerima JSON mentah
     * {@code {"timestamp":...,"status":500}} alih-alih halaman error aplikasi.
     */
    @Test
    @DisplayName("Oracle mati (gagal buka transaksi) tetap menghasilkan halaman error HTML, bukan JSON")
    void databaseMati_gagalBukaTransaksi_renderHalamanError() throws Exception {
        given(service.findAll()).willThrow(
                new CannotCreateTransactionException("Could not open JDBC Connection for transaction"));

        mvc.perform(get("/"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Gagal mengakses database")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Oracle Database sedang berjalan")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Kembali ke Daftar Mahasiswa")))
                // bukan respons JSON bawaan Spring
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("\"timestamp\""))))
                // detail teknis tidak bocor ke peramban
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ORA-"))))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("org.springframework"))));
    }

    @Test
    @DisplayName("Kegagalan saat query berjalan juga menghasilkan halaman error HTML")
    void queryGagal_renderHalamanError() throws Exception {
        given(service.findAll()).willThrow(
                new DataAccessResourceFailureException("ORA-00942: table or view does not exist"));

        mvc.perform(get("/"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Gagal mengakses database")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("ORA-00942"))));
    }

    @Test
    @DisplayName("URL yang tidak dikenal juga mendapat halaman 404 yang layak")
    void urlTidakDikenal_render404() throws Exception {
        mvc.perform(get("/halaman-yang-tidak-ada"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Mahasiswa tidak ditemukan")))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("Kembali ke Daftar Mahasiswa")));
    }
}
