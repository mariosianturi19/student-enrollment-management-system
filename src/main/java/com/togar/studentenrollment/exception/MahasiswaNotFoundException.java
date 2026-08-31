package com.togar.studentenrollment.exception;

/** Dilempar saat NIM yang diminta tidak ada di database. Ditangani jadi halaman 404. */
public class MahasiswaNotFoundException extends RuntimeException {

    private final String nim;

    public MahasiswaNotFoundException(String nim) {
        super("Mahasiswa dengan NIM " + nim + " tidak ditemukan");
        this.nim = nim;
    }

    public String getNim() {
        return nim;
    }
}
