package com.togar.studentenrollment.service;

import com.togar.studentenrollment.dto.MahasiswaDetail;
import com.togar.studentenrollment.exception.DuplicateNimException;
import com.togar.studentenrollment.exception.MahasiswaNotFoundException;
import com.togar.studentenrollment.model.Mahasiswa;
import com.togar.studentenrollment.repository.MahasiswaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Aturan bisnis seputar data mahasiswa.
 *
 * <p>Lapisan ini yang memutuskan kapan sebuah operasi sah — misalnya menolak NIM
 * ganda atau NIM yang tidak ada — sehingga controller cukup menerjemahkan
 * keputusan itu menjadi respons HTTP, dan repository cukup menjalankan SQL.
 */
@Service
public class MahasiswaService {

    private final MahasiswaRepository repository;

    public MahasiswaService(MahasiswaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Mahasiswa> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Mahasiswa findByNim(String nim) {
        return repository.findByNim(nim)
                .orElseThrow(() -> new MahasiswaNotFoundException(nim));
    }

    /**
     * Detail mahasiswa beserta mata kuliahnya, dari satu query JOIN.
     *
     * @throws MahasiswaNotFoundException bila NIM tidak ada
     */
    @Transactional(readOnly = true)
    public MahasiswaDetail findDetail(String nim) {
        return repository.findDetailByNim(nim)
                .orElseThrow(() -> new MahasiswaNotFoundException(nim));
    }

    @Transactional(readOnly = true)
    public boolean existsByNim(String nim) {
        return repository.existsByNim(nim);
    }

    /**
     * @throws DuplicateNimException bila NIM sudah dipakai
     */
    @Transactional
    public void create(Mahasiswa mahasiswa) {
        if (repository.existsByNim(mahasiswa.nim())) {
            throw new DuplicateNimException(mahasiswa.nim());
        }
        repository.insert(mahasiswa);
    }

    /**
     * Memperbarui data mahasiswa. NIM boleh diubah selama NIM barunya belum dipakai.
     *
     * @param originalNim NIM sebelum diubah
     * @throws MahasiswaNotFoundException bila {@code originalNim} tidak ada
     * @throws DuplicateNimException      bila NIM diubah ke nilai milik mahasiswa lain
     */
    @Transactional
    public void update(String originalNim, Mahasiswa mahasiswa) {
        if (!repository.existsByNim(originalNim)) {
            throw new MahasiswaNotFoundException(originalNim);
        }
        boolean nimBerubah = !originalNim.equals(mahasiswa.nim());
        if (nimBerubah && repository.existsByNim(mahasiswa.nim())) {
            throw new DuplicateNimException(mahasiswa.nim());
        }
        repository.update(originalNim, mahasiswa);
    }

    /**
     * Menghapus mahasiswa. Baris IRS miliknya ikut terhapus melalui
     * {@code ON DELETE CASCADE} pada constraint {@code fk_irs_mahasiswa}.
     *
     * @throws MahasiswaNotFoundException bila NIM tidak ada
     */
    @Transactional
    public void delete(String nim) {
        if (repository.deleteByNim(nim) == 0) {
            throw new MahasiswaNotFoundException(nim);
        }
    }
}
