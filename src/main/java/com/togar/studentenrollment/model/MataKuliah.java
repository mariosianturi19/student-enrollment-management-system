package com.togar.studentenrollment.model;

/** Baris tabel MATA_KULIAH. */
public record MataKuliah(
        String matkulId,
        String matkulNama,
        int sks,
        String hari
) {
}
