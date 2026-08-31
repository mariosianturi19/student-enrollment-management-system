package com.togar.studentenrollment.controller;

import com.togar.studentenrollment.config.WebBindingConfig;
import com.togar.studentenrollment.config.WebMvcConfig;
import com.togar.studentenrollment.dto.EnrolledCourse;
import com.togar.studentenrollment.dto.MahasiswaDetail;
import com.togar.studentenrollment.exception.DuplicateNimException;
import com.togar.studentenrollment.exception.MahasiswaNotFoundException;
import com.togar.studentenrollment.model.Mahasiswa;
import com.togar.studentenrollment.service.MahasiswaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Menguji lapisan web dengan {@link MahasiswaService} tiruan.
 *
 * <p>{@code @WebMvcTest} hanya menyalakan komponen Spring MVC: tidak ada
 * {@code DataSource}, tidak ada koneksi JDBC, tidak ada Oracle. Karena itu
 * seluruh kelas ini berjalan di komputer yang belum memasang Oracle sekalipun.
 *
 * <p>{@link WebBindingConfig} ikut diimpor agar perilaku pemangkasan spasi
 * benar-benar diuji, bukan diasumsikan.
 */
@WebMvcTest(MahasiswaController.class)
@Import({WebBindingConfig.class, WebMvcConfig.class, GlobalExceptionHandler.class})
class MahasiswaControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MahasiswaService service;

    private static final Mahasiswa SEKAR =
            new Mahasiswa("24060122001", "Sekar Ayu Pramesti", 2022, "P");

    // ----------------------------------------------------- daftar mahasiswa

    @Test
    @DisplayName("GET / menampilkan daftar mahasiswa dari service")
    void list_tampilkanDaftar() throws Exception {
        given(service.findAll()).willReturn(List.of(SEKAR));

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/list"))
                .andExpect(model().attributeExists("students"));
    }

    @Test
    @DisplayName("GET / tetap 200 saat belum ada data (empty state)")
    void list_kosong_tetapOk() throws Exception {
        given(service.findAll()).willReturn(List.of());

        mvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/list"))
                .andExpect(model().attribute("students", List.of()));
    }

    // ------------------------------------------------------------- detail

    @Test
    @DisplayName("GET /students/{nim} menampilkan mata kuliah hasil JOIN")
    void detail_tampilkanMataKuliah() throws Exception {
        MahasiswaDetail detail = new MahasiswaDetail(SEKAR, List.of(
                new EnrolledCourse(1001, "IF2101", "Sistem Basis Data", 3, "Senin", "aktif"),
                new EnrolledCourse(1002, "IF2102", "PBO", 3, "Selasa", "lulus")));
        given(service.findDetail("24060122001")).willReturn(detail);

        mvc.perform(get("/students/24060122001"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/detail"))
                .andExpect(model().attribute("mahasiswa", SEKAR))
                .andExpect(model().attribute("courses", detail.courses()));
    }

    @Test
    @DisplayName("GET /students/{nim} untuk mahasiswa tanpa IRS tetap 200 dengan daftar kosong")
    void detail_tanpaIrs_tetapOk() throws Exception {
        given(service.findDetail("24060123002"))
                .willReturn(new MahasiswaDetail(
                        new Mahasiswa("24060123002", "Dian Kusuma", 2023, "P"), List.of()));

        mvc.perform(get("/students/24060123002"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/detail"))
                .andExpect(model().attribute("courses", List.of()));
    }

    @Test
    @DisplayName("GET /students/{nim} yang tidak ada menghasilkan 404 dengan halaman sendiri")
    void detail_tidakAda_balas404() throws Exception {
        given(service.findDetail("99999999"))
                .willThrow(new MahasiswaNotFoundException("99999999"));

        mvc.perform(get("/students/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"))
                .andExpect(model().attribute("nim", "99999999"));
    }

    // -------------------------------------------------------- form tambah

    @Test
    @DisplayName("GET /students/new menampilkan form kosong dalam mode new")
    void formTambah_tampil() throws Exception {
        mvc.perform(get("/students/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/form"))
                .andExpect(model().attribute("mode", "new"))
                .andExpect(model().attributeExists("form"));
    }

    @Test
    @DisplayName("POST /students dengan data valid menyimpan lalu redirect ke daftar")
    void create_valid_redirect() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "24060122001")
                        .param("nama", "Sekar Ayu Pramesti")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("flashSuccess"));

        verify(service).create(SEKAR);
    }

    @Test
    @DisplayName("POST /students dengan seluruh field kosong menampilkan error tiap field")
    void create_semuaKosong_tampilkanErrorPerField() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "")
                        .param("nama", "")
                        .param("angkatan", "")
                        .param("gender", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("students/form"))
                .andExpect(model().attribute("mode", "new"))
                .andExpect(model().attributeHasFieldErrors("form", "nim", "nama", "angkatan", "gender"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /students dengan nama berisi spasi saja ditolak (dipangkas jadi kosong)")
    void create_namaHanyaSpasi_ditolak() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "24060122001")
                        .param("nama", "     ")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nama"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /students dengan NIM berisi huruf ditolak")
    void create_nimBukanAngka_ditolak() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "24060122ABC")
                        .param("nama", "Sekar Ayu Pramesti")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "nim"));

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST /students dengan angkatan di luar rentang ditolak")
    void create_angkatanDiLuarRentang_ditolak() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "24060122001")
                        .param("nama", "Sekar Ayu Pramesti")
                        .param("angkatan", "1990")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "angkatan"));
    }

    @Test
    @DisplayName("POST /students dengan gender di luar L/P ditolak")
    void create_genderTidakValid_ditolak() throws Exception {
        mvc.perform(post("/students")
                        .param("nim", "24060122001")
                        .param("nama", "Sekar Ayu Pramesti")
                        .param("angkatan", "2022")
                        .param("gender", "X"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "gender"));
    }

    @Test
    @DisplayName("POST /students dengan NIM duplikat menampilkan error pada field nim, bukan halaman error")
    void create_nimDuplikat_errorPadaFieldNim() throws Exception {
        willThrow(new DuplicateNimException("24060122001"))
                .given(service).create(any());

        mvc.perform(post("/students")
                        .param("nim", "24060122001")
                        .param("nama", "Sekar Ayu Pramesti")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/form"))
                .andExpect(model().attributeHasFieldErrors("form", "nim"));
    }

    // ---------------------------------------------------------- form edit

    @Test
    @DisplayName("GET /students/{nim}/edit menampilkan form terisi dalam mode edit")
    void formEdit_tampilTerisi() throws Exception {
        given(service.findByNim("24060122001")).willReturn(SEKAR);

        mvc.perform(get("/students/24060122001/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/form"))
                .andExpect(model().attribute("mode", "edit"))
                .andExpect(model().attribute("originalNim", "24060122001"));
    }

    @Test
    @DisplayName("GET /students/{nim}/edit untuk NIM tidak dikenal menghasilkan 404")
    void formEdit_tidakAda_balas404() throws Exception {
        given(service.findByNim("99999999"))
                .willThrow(new MahasiswaNotFoundException("99999999"));

        mvc.perform(get("/students/99999999/edit"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("POST /students/{nim}/edit dengan data valid menyimpan lalu redirect")
    void update_valid_redirect() throws Exception {
        mvc.perform(post("/students/24060122001/edit")
                        .param("nim", "24060122001")
                        .param("nama", "Sekar Ayu P.")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("flashSuccess"));

        verify(service).update("24060122001",
                new Mahasiswa("24060122001", "Sekar Ayu P.", 2022, "P"));
    }

    @Test
    @DisplayName("update memakai NIM dari path dan mengabaikan NIM di body (NIM tidak bisa diubah)")
    void update_nimDiBodyDiabaikan_pakaiNimPath() throws Exception {
        // NIM di body dipalsukan ke nilai lain; controller harus tetap memakai
        // NIM dari path sehingga primary key tidak pernah tersentuh.
        mvc.perform(post("/students/24060122001/edit")
                        .param("nim", "99999999")
                        .param("nama", "Sekar Ayu P.")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        verify(service).update("24060122001",
                new Mahasiswa("24060122001", "Sekar Ayu P.", 2022, "P"));
    }

    @Test
    @DisplayName("POST /students/{nim}/edit yang tidak valid kembali ke form dengan mode edit utuh")
    void update_tidakValid_kembaliKeForm() throws Exception {
        mvc.perform(post("/students/24060122001/edit")
                        .param("nim", "24060122001")
                        .param("nama", "")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/form"))
                .andExpect(model().attribute("mode", "edit"))
                .andExpect(model().attribute("originalNim", "24060122001"))
                .andExpect(model().attributeHasFieldErrors("form", "nama"));

        verify(service, never()).update(anyString(), any());
    }

    /**
     * NIM sudah tidak bisa diubah lewat form edit (field-nya {@code readonly}
     * dan controller memakai NIM dari path, bukan dari body). Skenario "ubah NIM
     * ke milik orang lain" karena itu tidak lagi terjangkau dari UI.
     *
     * <p>Test ini tetap ada untuk mengunci pertahanan lapis bawah: bila
     * {@code MahasiswaService} tetap melempar {@link DuplicateNimException} (mis.
     * dipanggil dari tempat lain di masa depan), controller menampilkannya
     * sebagai error field, bukan halaman 500.
     */
    @Test
    @DisplayName("update: DuplicateNimException dari service tampil sebagai error field nim, bukan 500")
    void update_serviceLemparDuplicate_errorPadaFieldNim() throws Exception {
        willThrow(new DuplicateNimException("24060122002"))
                .given(service).update(anyString(), any());

        mvc.perform(post("/students/24060122001/edit")
                        .param("nim", "24060122001")
                        .param("nama", "Sekar Ayu Pramesti")
                        .param("angkatan", "2022")
                        .param("gender", "P"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/form"))
                .andExpect(model().attributeHasFieldErrors("form", "nim"));
    }

    // ------------------------------------------------------------- hapus

    @Test
    @DisplayName("POST /students/{nim}/delete menghapus lalu redirect ke daftar")
    void delete_viaPost_redirect() throws Exception {
        mvc.perform(post("/students/24060122001/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("flashSuccess"));

        verify(service).delete("24060122001");
    }

    @Test
    @DisplayName("GET /students/{nim}/delete TIDAK menghapus apa pun — hanya POST yang diterima")
    void delete_viaGet_ditolak() throws Exception {
        mvc.perform(get("/students/24060122001/delete"))
                .andExpect(status().isMethodNotAllowed());

        verify(service, never()).delete(anyString());
    }

    @Test
    @DisplayName("POST /students/{nim}/delete untuk NIM tidak dikenal menghasilkan 404")
    void delete_tidakAda_balas404() throws Exception {
        doThrow(new MahasiswaNotFoundException("99999999"))
                .when(service).delete("99999999");

        mvc.perform(post("/students/99999999/delete"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    // ------------------------------------------------- regresi: URL jsessionid

    /**
     * Regresi untuk bug yang ditemukan saat pengujian dengan Oracle sungguhan.
     *
     * <p>Pada POST pertama di sesi yang benar-benar baru, peramban belum
     * mengirim cookie apa pun, sehingga Tomcat menambahkan session ke URL:
     * {@code Location: /;jsessionid=...}. Padahal {@code @GetMapping("/")}
     * tidak cocok dengan path yang membawa parameter matriks itu, dan karena
     * {@code throw-exception-if-no-handler-found=true} hasilnya 404 — data
     * mahasiswa sudah tersimpan, tetapi pengguna justru melihat halaman
     * "Tidak Ditemukan".
     *
     * <p>Perbaikannya mematikan URL rewriting ({@code server.servlet.session
     * .tracking-modes=cookie}), sehingga Location selalu bersih.
     */
    @Test
    @DisplayName("Halaman daftar tetap dapat diakses walau URL membawa ;jsessionid")
    void daftar_denganJsessionidDiUrl_tetap200() throws Exception {
        given(service.findAll()).willReturn(List.of(SEKAR));

        mvc.perform(get("/;jsessionid=A1B2C3D4E5F6"))
                .andExpect(status().isOk())
                .andExpect(view().name("students/list"));
    }
}
