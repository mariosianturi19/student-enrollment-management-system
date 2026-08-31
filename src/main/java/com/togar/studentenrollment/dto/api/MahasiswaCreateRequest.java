package com.togar.studentenrollment.dto.api;

import com.togar.studentenrollment.model.Mahasiswa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body untuk {@code POST /api/students}.
 *
 * <p>Sengaja terpisah dari {@code MahasiswaForm}: form itu kelas mutable dengan
 * getter/setter karena Spring MVC membutuhkannya untuk data binding HTML,
 * sedangkan body JSON bisa langsung dipetakan ke record yang immutable.
 * Batas validasinya dibuat sama persis, sehingga kedua pintu masuk menolak data
 * yang sama.
 *
 * <p>{@code angkatan} bertipe {@link Integer}, bukan {@code int}, supaya field
 * yang tidak dikirim menjadi {@code null} dan tertangkap {@code @NotNull} dengan
 * pesan yang ramah, bukan gagal deserialisasi dengan pesan teknis.
 */
public record MahasiswaCreateRequest(

        @NotBlank(message = "NIM wajib diisi")
        @Pattern(regexp = "\\d{8,20}", message = "NIM harus berupa 8 sampai 20 digit angka")
        String nim,

        @NotBlank(message = "Nama wajib diisi")
        @Size(max = 100, message = "Nama maksimal 100 karakter")
        String nama,

        @NotNull(message = "Angkatan wajib diisi")
        @Min(value = 2000, message = "Angkatan paling awal adalah 2000")
        @Max(value = 2100, message = "Angkatan paling akhir adalah 2100")
        Integer angkatan,

        @NotBlank(message = "Gender wajib dipilih")
        @Pattern(regexp = "[LP]", message = "Gender harus L atau P")
        String gender
) {

    /** Hanya dipanggil setelah validasi lolos, sehingga {@code angkatan} pasti tidak null. */
    public Mahasiswa toMahasiswa() {
        return new Mahasiswa(nim, nama, angkatan, gender);
    }
}
