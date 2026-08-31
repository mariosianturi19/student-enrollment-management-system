package com.togar.studentenrollment.controller.api;

import com.togar.studentenrollment.dto.api.ApiError;
import com.togar.studentenrollment.exception.DuplicateNimException;
import com.togar.studentenrollment.exception.MahasiswaNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mengubah kegagalan di bawah {@code /api} menjadi JSON.
 *
 * <p><strong>Kenapa advice terpisah.</strong> {@code GlobalExceptionHandler}
 * mengembalikan nama view Thymeleaf. Tanpa kelas ini, sebuah
 * {@link MahasiswaNotFoundException} dari controller API akan dirender menjadi
 * halaman HTML 404 dan dikirim ke pemanggil yang meminta JSON.
 *
 * <p><strong>Kenapa dibatasi paket, bukan menggantikan yang lama.</strong> Advice
 * ini hanya berlaku untuk controller di paket {@code controller.api}, dan
 * {@code @Order} tertinggi membuatnya menang ketika keduanya sama-sama cocok.
 * Advice yang lama sengaja dibiarkan berlaku global, karena ia juga menangani
 * {@code NoResourceFoundException} yang dilempar di luar controller mana pun
 * (URL salah ketik) dan halaman 404 itu harus tetap ada.
 */
@RestControllerAdvice(basePackages = "com.togar.studentenrollment.controller.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MahasiswaNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(MahasiswaNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "Not Found", e.getMessage()));
    }

    /**
     * NIM ganda dibalas 409, bukan 400.
     *
     * <p>Body-nya sendiri sah: bentuknya benar dan lolos seluruh validasi. Yang
     * bertabrakan adalah keadaan di database, dan permintaan yang sama persis bisa
     * saja berhasil setelah baris yang bentrok dihapus. Itulah arti 409.
     */
    @ExceptionHandler(DuplicateNimException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateNimException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "Conflict", e.getMessage()));
    }

    /**
     * Kegagalan bean validation, dilaporkan per field.
     *
     * <p>Satu pesan gabungan memaksa klien mengurai teks untuk tahu field mana yang
     * salah. Peta ini bisa langsung dipetakan ke input yang bersangkutan.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        // merge: sebuah field bisa melanggar lebih dari satu constraint; pesan
        // pertama sudah cukup untuk diperbaiki, dan menumpuknya hanya membebani klien.
        e.getBindingResult().getFieldErrors().forEach(fe ->
                fieldErrors.merge(fe.getField(), fe.getDefaultMessage(), (first, next) -> first));

        return ResponseEntity.badRequest()
                .body(ApiError.validation(400, "Data yang dikirim tidak valid", fieldErrors));
    }

    /** JSON rusak, atau tipe field tidak cocok sehingga gagal sebelum validasi berjalan. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "Bad Request", "Body permintaan bukan JSON yang valid."));
    }

    /**
     * Kegagalan database. Ditangani di sini juga supaya pemanggil API tetap
     * menerima JSON, bukan halaman "Gagal mengakses database" milik sisi MVC.
     *
     * <p>Alasan {@link TransactionException} ikut ditangani sama dengan di
     * {@code GlobalExceptionHandler}: ketika Oracle mati sama sekali,
     * {@code @Transactional} gagal membuka koneksi lebih dulu dan melempar
     * {@code CannotCreateTransactionException}, yang bukan turunan
     * {@link DataAccessException}.
     */
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    public ResponseEntity<ApiError> handleDatabase(Exception e) {
        log.error("Kegagalan akses database pada API", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "Internal Server Error",
                        "Tidak dapat mengakses database."));
    }
}
