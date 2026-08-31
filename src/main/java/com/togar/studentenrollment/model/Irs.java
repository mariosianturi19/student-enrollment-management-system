package com.togar.studentenrollment.model;

/** Baris tabel IRS (Isian Rencana Studi). */
public record Irs(
        long irsId,
        String nim,
        String matkulId,
        String status
) {
}
