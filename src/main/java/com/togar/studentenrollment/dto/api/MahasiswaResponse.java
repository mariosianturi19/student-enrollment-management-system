package com.togar.studentenrollment.dto.api;

import com.togar.studentenrollment.model.Mahasiswa;

/**
 * Bentuk satu mahasiswa pada respons API.
 *
 * <p>Record {@link Mahasiswa} sengaja tidak diserialisasi langsung. Model itu
 * mencerminkan baris tabel, dan menjadikannya bentuk respons berarti perubahan
 * skema database ikut mengubah kontrak API tanpa ada yang menyadarinya. Lapisan
 * tipis ini membuat kedua hal itu bisa berubah sendiri-sendiri.
 *
 * <p>{@code genderLabel} disertakan supaya klien tidak perlu menduplikasi
 * pemetaan {@code L} dan {@code P} ke teks yang layak tampil.
 */
public record MahasiswaResponse(
        String nim,
        String nama,
        int angkatan,
        String gender,
        String genderLabel
) {

    public static MahasiswaResponse from(Mahasiswa m) {
        return new MahasiswaResponse(m.nim(), m.nama(), m.angkatan(), m.gender(), m.genderLabel());
    }
}
