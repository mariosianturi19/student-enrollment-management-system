package com.togar.studentenrollment.dto.api;

import com.togar.studentenrollment.dto.EnrolledCourse;

/**
 * Satu mata kuliah yang diambil, pada respons API.
 *
 * <p>{@code statusBadgeClass} milik {@link EnrolledCourse} sengaja tidak ikut:
 * nama kelas CSS adalah urusan tampilan Thymeleaf, dan mengirimkannya lewat API
 * berarti memaksakan pilihan styling aplikasi ini kepada setiap klien.
 */
public record EnrolledCourseResponse(
        long irsId,
        String matkulId,
        String matkulNama,
        int sks,
        String hari,
        String status
) {

    public static EnrolledCourseResponse from(EnrolledCourse c) {
        return new EnrolledCourseResponse(
                c.irsId(), c.matkulId(), c.matkulNama(), c.sks(), c.hari(), c.status());
    }
}
