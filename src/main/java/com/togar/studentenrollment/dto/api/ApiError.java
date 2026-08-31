package com.togar.studentenrollment.dto.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Bentuk tunggal untuk semua respons error API.
 *
 * <p>Satu bentuk untuk setiap kegagalan berarti klien cukup menulis satu jalur
 * penanganan error. Tanpa ini, Spring mengembalikan bentuk bawaan yang berbeda
 * tergantung di lapisan mana kegagalannya terjadi.
 *
 * <p>{@code fieldErrors} hanya muncul pada kegagalan validasi; {@code @JsonInclude}
 * menghilangkannya dari kasus lain supaya klien tidak menemukan objek kosong yang
 * membingungkan.
 *
 * <p>Sama seperti sisi MVC, tidak ada stack trace, nama kelas internal, maupun
 * kode {@code ORA-} yang keluar dari sini.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, null);
    }

    public static ApiError validation(int status, String message, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, "Bad Request", message, fieldErrors);
    }
}
