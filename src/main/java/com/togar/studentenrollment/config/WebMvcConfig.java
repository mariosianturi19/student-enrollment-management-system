package com.togar.studentenrollment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

/**
 * Konfigurasi pencocokan URL.
 *
 * <p>Menghapus parameter matriks (segmen setelah {@code ;} pada sebuah path)
 * sebelum URL dicocokkan ke handler. Yang paling sering muncul adalah
 * {@code ;jsessionid=...} yang disisipkan Tomcat pada request pertama sebuah
 * sesi, ketika peramban belum sempat mengirim cookie.
 *
 * <p>Tanpa ini, {@code redirect:/} setelah menambah mahasiswa bisa mendarat di
 * {@code /;jsessionid=...}. Path itu tidak cocok dengan {@code @GetMapping("/")},
 * dan karena {@code spring.mvc.throw-exception-if-no-handler-found=true}
 * hasilnya halaman 404 — padahal data mahasiswanya sudah tersimpan. Bug ini
 * ditemukan saat pengujian dengan Oracle sungguhan, bukan saat menulis kode.
 *
 * <p>Ini lapisan kedua: {@code server.servlet.session.tracking-modes=cookie} di
 * {@code application.properties} sudah mencegah URL semacam itu dibuat, dan
 * konfigurasi ini membuat aplikasi tetap benar bila URL seperti itu tetap
 * datang — misalnya dari bookmark lama atau tautan yang pernah disalin.
 *
 * <p>Dikunci oleh test {@code MahasiswaControllerTest
 * .daftar_denganJsessionidDiUrl_tetap200}.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        UrlPathHelper pathHelper = new UrlPathHelper();
        pathHelper.setRemoveSemicolonContent(true);
        configurer.setUrlPathHelper(pathHelper);
    }
}
