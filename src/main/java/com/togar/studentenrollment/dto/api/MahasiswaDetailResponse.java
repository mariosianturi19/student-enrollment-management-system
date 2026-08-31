package com.togar.studentenrollment.dto.api;

import com.togar.studentenrollment.dto.MahasiswaDetail;

import java.util.List;

/**
 * Bentuk {@code GET /api/students/{nim}}: mahasiswa beserta mata kuliah yang
 * diambilnya, hasil satu query JOIN.
 *
 * <p>{@code totalSks} ikut dikirim karena sudah dihitung di sisi server dan
 * membiarkan setiap klien menjumlahkannya sendiri hanya mengundang
 * ketidakcocokan.
 *
 * <p>Mahasiswa tanpa mata kuliah menghasilkan array kosong, bukan {@code null},
 * sehingga klien tidak perlu membedakan dua kasus yang artinya sama.
 */
public record MahasiswaDetailResponse(
        String nim,
        String nama,
        int angkatan,
        String gender,
        String genderLabel,
        int totalSks,
        List<EnrolledCourseResponse> courses
) {

    public static MahasiswaDetailResponse from(MahasiswaDetail detail) {
        var m = detail.mahasiswa();
        return new MahasiswaDetailResponse(
                m.nim(),
                m.nama(),
                m.angkatan(),
                m.gender(),
                m.genderLabel(),
                detail.totalSks(),
                detail.courses().stream().map(EnrolledCourseResponse::from).toList());
    }
}
