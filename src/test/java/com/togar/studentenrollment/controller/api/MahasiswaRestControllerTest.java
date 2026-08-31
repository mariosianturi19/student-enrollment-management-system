package com.togar.studentenrollment.controller.api;

import com.togar.studentenrollment.config.WebBindingConfig;
import com.togar.studentenrollment.controller.GlobalExceptionHandler;
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
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Menguji lapisan REST dengan {@link MahasiswaService} tiruan.
 *
 * <p>Seperti test sisi MVC, {@code @WebMvcTest} hanya menyalakan komponen Spring
 * MVC: tidak ada {@code DataSource} dan tidak ada Oracle, sehingga kelas ini
 * berjalan di komputer yang belum memasang database sekalipun.
 *
 * <p><strong>Kedua exception advice sengaja diimpor bersamaan.</strong> Mengimpor
 * {@link ApiExceptionHandler} saja akan menguji dunia yang tidak pernah ada:
 * di aplikasi sungguhan {@code GlobalExceptionHandler} juga terdaftar dan
 * berlaku global. Dengan keduanya hadir, setiap pemeriksaan JSON di bawah ini
 * sekaligus membuktikan advice API yang menang, sehingga pemanggil API tidak
 * pernah menerima halaman HTML.
 */
@WebMvcTest(MahasiswaRestController.class)
@Import({WebBindingConfig.class, ApiExceptionHandler.class, GlobalExceptionHandler.class})
class MahasiswaRestControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private MahasiswaService service;

    private static final Mahasiswa SEKAR =
            new Mahasiswa("24060122001", "Sekar Ayu Pramesti", 2022, "P");

    private static final String VALID_BODY = """
            {"nim":"24060122001","nama":"Sekar Ayu Pramesti","angkatan":2022,"gender":"P"}
            """;

    // ------------------------------------------------------------------ list

    @Test
    @DisplayName("GET /api/students mengembalikan daftar sebagai JSON")
    void list_kembalikanJson() throws Exception {
        given(service.findAll()).willReturn(List.of(SEKAR));

        mvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].nim").value("24060122001"))
                .andExpect(jsonPath("$[0].genderLabel").value("Perempuan"));
    }

    @Test
    @DisplayName("GET /api/students pada database kosong mengembalikan array kosong, bukan 404")
    void list_kosong_arrayKosong() throws Exception {
        given(service.findAll()).willReturn(List.of());

        mvc.perform(get("/api/students"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ---------------------------------------------------------------- detail

    @Test
    @DisplayName("GET /api/students/{nim} menyertakan mata kuliah dan total SKS")
    void detail_sertakanMataKuliah() throws Exception {
        given(service.findDetail("24060122001")).willReturn(new MahasiswaDetail(SEKAR, List.of(
                new EnrolledCourse(1001, "IF2101", "Sistem Basis Data", 3, "Senin", "aktif"),
                new EnrolledCourse(1002, "IF2102", "PBO", 3, "Selasa", "lulus"))));

        mvc.perform(get("/api/students/24060122001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nim").value("24060122001"))
                .andExpect(jsonPath("$.totalSks").value(6))
                .andExpect(jsonPath("$.courses.length()").value(2))
                .andExpect(jsonPath("$.courses[0].matkulId").value("IF2101"));
    }

    @Test
    @DisplayName("Mahasiswa tanpa mata kuliah menghasilkan array kosong, bukan null")
    void detail_tanpaMataKuliah_arrayKosong() throws Exception {
        given(service.findDetail("24060123002")).willReturn(new MahasiswaDetail(
                new Mahasiswa("24060123002", "Dian Kusuma", 2023, "P"), List.of()));

        mvc.perform(get("/api/students/24060123002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courses").isArray())
                .andExpect(jsonPath("$.courses").isEmpty())
                .andExpect(jsonPath("$.totalSks").value(0));
    }

    @Test
    @DisplayName("NIM tidak dikenal menghasilkan 404 berbentuk JSON, bukan halaman HTML")
    void detail_tidakAda_404Json() throws Exception {
        given(service.findDetail("99999999"))
                .willThrow(new MahasiswaNotFoundException("99999999"));

        mvc.perform(get("/api/students/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("99999999")));
    }

    // ---------------------------------------------------------------- create

    @Test
    @DisplayName("POST membalas 201 dengan header Location")
    void create_valid_201DenganLocation() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/students/24060122001"))
                .andExpect(jsonPath("$.nim").value("24060122001"));

        verify(service).create(SEKAR);
    }

    @Test
    @DisplayName("POST dengan seluruh field kosong membalas 400 dengan error per field")
    void create_kosong_400PerField() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nim").exists())
                .andExpect(jsonPath("$.fieldErrors.nama").exists())
                .andExpect(jsonPath("$.fieldErrors.angkatan").exists())
                .andExpect(jsonPath("$.fieldErrors.gender").exists());

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST dengan NIM berisi huruf ditolak 400")
    void create_nimBukanAngka_400() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nim":"24060122ABC","nama":"Sekar","angkatan":2022,"gender":"P"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nim").exists());

        verify(service, never()).create(any());
    }

    @Test
    @DisplayName("POST dengan angkatan di luar rentang ditolak 400")
    void create_angkatanDiLuarRentang_400() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nim":"24060122001","nama":"Sekar","angkatan":1990,"gender":"P"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.angkatan").exists());
    }

    @Test
    @DisplayName("POST dengan NIM yang sudah ada membalas 409, bukan 400")
    void create_nimDuplikat_409() throws Exception {
        willThrow(new DuplicateNimException("24060122001")).given(service).create(any());

        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("POST dengan JSON rusak membalas 400, bukan 500")
    void create_jsonRusak_400() throws Exception {
        mvc.perform(post("/api/students")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verify(service, never()).create(any());
    }

    // ---------------------------------------------------------------- update

    @Test
    @DisplayName("PUT memperbarui data dan memakai NIM dari path")
    void update_valid_pakaiNimPath() throws Exception {
        mvc.perform(put("/api/students/24060122001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nama":"Sekar Ayu P.","angkatan":2022,"gender":"P"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nim").value("24060122001"))
                .andExpect(jsonPath("$.nama").value("Sekar Ayu P."));

        verify(service).update("24060122001",
                new Mahasiswa("24060122001", "Sekar Ayu P.", 2022, "P"));
    }

    /**
     * Bentuk body update memang tidak punya field {@code nim}, jadi mengirimkannya
     * tidak berpengaruh apa pun. Test ini mengunci sifat itu: primary key tidak bisa
     * ditulis ulang lewat API, sama seperti lewat form.
     */
    @Test
    @DisplayName("PUT mengabaikan nim yang diselipkan di body")
    void update_nimDiBody_diabaikan() throws Exception {
        mvc.perform(put("/api/students/24060122001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nim":"99999999","nama":"Sekar Ayu P.","angkatan":2022,"gender":"P"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nim").value("24060122001"));

        verify(service).update("24060122001",
                new Mahasiswa("24060122001", "Sekar Ayu P.", 2022, "P"));
    }

    @Test
    @DisplayName("PUT untuk NIM tidak dikenal membalas 404")
    void update_tidakAda_404() throws Exception {
        willThrow(new MahasiswaNotFoundException("99999999"))
                .given(service).update(anyString(), any());

        mvc.perform(put("/api/students/99999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nama":"Sekar","angkatan":2022,"gender":"P"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("PUT dengan nama kosong ditolak 400 dan tidak menyentuh service")
    void update_namaKosong_400() throws Exception {
        mvc.perform(put("/api/students/24060122001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nama":"","angkatan":2022,"gender":"P"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.nama").exists());

        verify(service, never()).update(anyString(), any());
    }

    // ---------------------------------------------------------------- delete

    @Test
    @DisplayName("DELETE membalas 204 tanpa body")
    void delete_valid_204() throws Exception {
        mvc.perform(delete("/api/students/24060122001"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service).delete("24060122001");
    }

    @Test
    @DisplayName("DELETE untuk NIM tidak dikenal membalas 404")
    void delete_tidakAda_404() throws Exception {
        doThrow(new MahasiswaNotFoundException("99999999")).when(service).delete("99999999");

        mvc.perform(delete("/api/students/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
