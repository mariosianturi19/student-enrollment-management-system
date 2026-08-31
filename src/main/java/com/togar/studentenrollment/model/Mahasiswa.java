package com.togar.studentenrollment.model;

/**
 * Baris tabel MAHASISWA.
 *
 * <p>Record (immutable) karena objek ini hanya dibaca dari / ditulis ke database
 * dan tidak pernah dimutasi setelah dibuat. Untuk data yang masuk dari form HTML
 * dipakai {@code MahasiswaForm} yang bisa berisi nilai belum tervalidasi.
 */
public record Mahasiswa(
        String nim,
        String nama,
        int angkatan,
        String gender
) {

    /** Label gender yang layak ditampilkan di layar. */
    public String genderLabel() {
        return switch (gender == null ? "" : gender.toUpperCase()) {
            case "L" -> "Laki-laki";
            case "P" -> "Perempuan";
            default -> "-";
        };
    }
}
