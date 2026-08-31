package com.togar.studentenrollment.controller;

import com.togar.studentenrollment.exception.MahasiswaNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Mengubah kegagalan menjadi halaman yang bisa dimengerti pengguna.
 *
 * <p>Pesan teknis (stack trace, kode error Oracle) hanya masuk ke log server;
 * yang tampil di layar adalah kalimat biasa tanpa detail internal — detail
 * seperti nama tabel atau kode ORA tidak perlu, dan tidak sepatutnya,
 * dibocorkan ke peramban.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MahasiswaNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(MahasiswaNotFoundException e, Model model) {
        model.addAttribute("nim", e.getNim());
        return "error/404";
    }

    /**
     * Alamat yang tidak dikenal sama sekali, misalnya salah ketik URL.
     * Ditangani di sini supaya pengguna mendapat halaman 404 yang sama rapinya,
     * bukan halaman kosong — {@code server.error.whitelabel.enabled} sengaja
     * dimatikan.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleUnknownUrl(NoResourceFoundException e) {
        // Tanpa atribut "nim": template menampilkan kalimat umum, bukan menyebut NIM.
        return "error/404";
    }

    /**
     * Kegagalan akses database: Oracle mati, kredensial salah, tabel belum dibuat,
     * atau constraint yang tidak tertangkap validasi.
     *
     * <p>Dua tipe exception ditangani sekaligus, dan keduanya memang perlu:
     * <ul>
     *   <li>{@link DataAccessException} — kegagalan saat query dijalankan
     *       (tabel tidak ada, constraint dilanggar);</li>
     *   <li>{@link TransactionException} — kegagalan saat transaksi dibuka,
     *       sebelum query sempat berjalan. Inilah yang terjadi ketika Oracle
     *       mati sama sekali: {@code @Transactional} pada service mencoba
     *       membuka koneksi lebih dulu dan gagal dengan
     *       {@code CannotCreateTransactionException}, yang <em>bukan</em>
     *       turunan {@code DataAccessException}. Tanpa penanganan ini, Spring
     *       jatuh ke respons JSON bawaan alih-alih halaman error aplikasi.</li>
     * </ul>
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleDatabaseError(Exception e, Model model) {
        log.error("Kegagalan akses database", e);
        // Nama atribut sengaja diberi awalan "error": parameter fragment layout
        // juga bernama "judul", dan variabel lokal fragment menutupi model
        // attribute bernama sama sehingga judulnya tidak pernah tampil.
        model.addAttribute("errorJudul", "Gagal mengakses database");
        model.addAttribute("errorPesan",
                "Aplikasi tidak dapat terhubung ke database. "
                        + "Pastikan Oracle Database sedang berjalan, environment variable "
                        + "DB_URL / DB_USERNAME / DB_PASSWORD sudah benar, dan schema.sql "
                        + "sudah dijalankan.");
        return "error/error";
    }
}
