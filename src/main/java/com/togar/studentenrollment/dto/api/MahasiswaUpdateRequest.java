package com.togar.studentenrollment.dto.api;

import com.togar.studentenrollment.model.Mahasiswa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Body untuk {@code PUT /api/students/{nim}}.
 *
 * <p><strong>Tidak ada field {@code nim} di sini, dan itu disengaja.</strong> NIM
 * adalah primary key sekaligus identitas mahasiswa, dan Oracle tidak mendukung
 * {@code ON UPDATE CASCADE}, sehingga mengubahnya untuk mahasiswa yang sudah punya
 * baris IRS akan gagal di tingkat constraint. Sisi MVC menguncinya dengan field
 * {@code readonly} pada form; di sisi API, bentuk body-nya sendiri yang
 * menyatakannya, jadi klien tidak perlu menebak apakah NIM boleh dikirim.
 *
 * <p>NIM tujuan selalu diambil dari path.
 */
public record MahasiswaUpdateRequest(

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

    /** @param nim diambil dari path, bukan dari body */
    public Mahasiswa toMahasiswa(String nim) {
        return new Mahasiswa(nim, nama, angkatan, gender);
    }
}
