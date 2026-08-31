package com.togar.studentenrollment.dto;

import com.togar.studentenrollment.model.Mahasiswa;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Objek pengikat form tambah/edit mahasiswa.
 *
 * <p>Sengaja dipisahkan dari record {@link Mahasiswa}:
 * <ul>
 *   <li>{@code angkatan} bertipe {@link Integer} (bukan {@code int}) supaya input
 *       kosong menjadi {@code null} dan tertangkap {@code @NotNull}, bukan gagal
 *       konversi dengan pesan teknis;</li>
 *   <li>Bean Validation butuh kelas mutable dengan getter/setter untuk data binding
 *       Spring MVC;</li>
 *   <li>form boleh berisi nilai yang belum valid, sedangkan {@code Mahasiswa}
 *       merepresentasikan data yang sudah sah di database.</li>
 * </ul>
 *
 * <p>Batas validasi di sini dibuat <em>sama atau lebih ketat</em> daripada CHECK
 * constraint di {@code schema.sql}, sehingga pengguna melihat pesan yang ramah
 * dan tidak pernah menabrak error ORA-02290 mentah.
 */
public class MahasiswaForm {

    @NotBlank(message = "NIM wajib diisi")
    @Pattern(regexp = "\\d{8,20}", message = "NIM harus berupa 8 sampai 20 digit angka")
    private String nim;

    @NotBlank(message = "Nama wajib diisi")
    @Size(max = 100, message = "Nama maksimal 100 karakter")
    private String nama;

    @NotNull(message = "Angkatan wajib diisi")
    @Min(value = 2000, message = "Angkatan paling awal adalah 2000")
    @Max(value = 2100, message = "Angkatan paling akhir adalah 2100")
    private Integer angkatan;

    @NotBlank(message = "Gender wajib dipilih")
    @Pattern(regexp = "[LP]", message = "Gender harus L atau P")
    private String gender;

    /**
     * NIM asli saat form edit dibuka. Dipakai controller untuk membedakan
     * "NIM tidak diubah" dari "NIM diubah ke nilai yang sudah dipakai orang lain".
     * Tidak divalidasi karena bukan input pengguna.
     */
    private String originalNim;

    public MahasiswaForm() {
    }

    public MahasiswaForm(String nim, String nama, Integer angkatan, String gender) {
        this.nim = nim;
        this.nama = nama;
        this.angkatan = angkatan;
        this.gender = gender;
    }

    /** Membangun form dari data yang sudah tersimpan, untuk halaman edit. */
    public static MahasiswaForm from(Mahasiswa m) {
        MahasiswaForm form = new MahasiswaForm(m.nim(), m.nama(), m.angkatan(), m.gender());
        form.setOriginalNim(m.nim());
        return form;
    }

    /**
     * Mengubah form yang sudah tervalidasi menjadi objek domain.
     * Hanya dipanggil setelah {@code BindingResult} bersih, sehingga
     * {@code angkatan} dipastikan tidak null.
     */
    public Mahasiswa toMahasiswa() {
        return new Mahasiswa(nim, nama, angkatan, gender);
    }

    public String getNim() {
        return nim;
    }

    public void setNim(String nim) {
        this.nim = nim;
    }

    public String getNama() {
        return nama;
    }

    public void setNama(String nama) {
        this.nama = nama;
    }

    public Integer getAngkatan() {
        return angkatan;
    }

    public void setAngkatan(Integer angkatan) {
        this.angkatan = angkatan;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getOriginalNim() {
        return originalNim;
    }

    public void setOriginalNim(String originalNim) {
        this.originalNim = originalNim;
    }
}
