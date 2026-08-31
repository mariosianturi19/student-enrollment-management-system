package com.togar.studentenrollment.exception;

/**
 * Dilempar saat NIM yang akan disimpan sudah dipakai mahasiswa lain.
 * Controller menangkapnya dan mengubahnya menjadi error pada field {@code nim},
 * bukan halaman error — pengguna harus bisa langsung memperbaiki isian.
 */
public class DuplicateNimException extends RuntimeException {

    private final String nim;

    public DuplicateNimException(String nim) {
        super("NIM " + nim + " sudah terdaftar");
        this.nim = nim;
    }

    public String getNim() {
        return nim;
    }
}
