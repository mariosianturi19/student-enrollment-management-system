package com.togar.studentenrollment.dto;

import com.togar.studentenrollment.model.Mahasiswa;

import java.util.List;

/**
 * Hasil satu query JOIN pada halaman detail mahasiswa: data mahasiswa
 * beserta seluruh mata kuliah yang diambilnya.
 *
 * <p>Dirakit oleh {@code MahasiswaRepository} dari satu {@code ResultSet},
 * sehingga tidak ada query tambahan per mata kuliah (menghindari N+1).
 *
 * @param courses daftar mata kuliah; kosong bila mahasiswa belum mengambil apa pun
 */
public record MahasiswaDetail(
        Mahasiswa mahasiswa,
        List<EnrolledCourse> courses
) {

    public MahasiswaDetail {
        courses = courses == null ? List.of() : List.copyOf(courses);
    }

    public boolean hasCourses() {
        return !courses.isEmpty();
    }

    /** Total SKS yang sedang atau pernah diambil. */
    public int totalSks() {
        return courses.stream().mapToInt(EnrolledCourse::sks).sum();
    }
}
