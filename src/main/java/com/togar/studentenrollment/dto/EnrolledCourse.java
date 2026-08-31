package com.togar.studentenrollment.dto;

/**
 * Satu baris pada tabel "Mata Kuliah yang Diambil" di halaman detail mahasiswa.
 *
 * <p>Ini adalah hasil gabungan kolom dari MATA_KULIAH dan IRS, bukan cerminan
 * satu tabel tunggal — karena itu diletakkan di paket {@code dto}, bukan
 * {@code model}.
 */
public record EnrolledCourse(
        long irsId,
        String matkulId,
        String matkulNama,
        int sks,
        String hari,
        String status
) {

    /**
     * Kelas CSS badge untuk status ini.
     * Nilai status di luar tiga yang dikenal tetap ditampilkan, tetapi
     * memakai gaya netral agar halaman tidak rusak.
     */
    public String statusBadgeClass() {
        return switch (status == null ? "" : status.toLowerCase()) {
            case "lulus" -> "badge-status badge-lulus";
            case "aktif" -> "badge-status badge-aktif";
            case "gagal" -> "badge-status badge-gagal";
            default -> "badge-status badge-netral";
        };
    }
}
