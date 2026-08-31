package com.togar.studentenrollment.controller.api;

import com.togar.studentenrollment.dto.api.MahasiswaCreateRequest;
import com.togar.studentenrollment.dto.api.MahasiswaDetailResponse;
import com.togar.studentenrollment.dto.api.MahasiswaResponse;
import com.togar.studentenrollment.dto.api.MahasiswaUpdateRequest;
import com.togar.studentenrollment.service.MahasiswaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST API untuk data mahasiswa.
 *
 * <p>Berdampingan dengan {@code MahasiswaController} yang merender HTML, dan
 * keduanya memanggil {@link MahasiswaService} yang sama. Aturan bisnisnya (NIM
 * ganda, NIM tidak ada, transaksi) hanya ditulis satu kali di service, jadi kedua
 * pintu masuk tidak mungkin berbeda perilaku.
 *
 * <p>Yang berbeda hanyalah cara menerjemahkan hasilnya: controller HTML mengubah
 * kegagalan menjadi halaman, controller ini mengubahnya menjadi status HTTP dan
 * JSON. Lihat {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/students")
public class MahasiswaRestController {

    private final MahasiswaService service;

    public MahasiswaRestController(MahasiswaService service) {
        this.service = service;
    }

    /** Daftar seluruh mahasiswa. Database kosong menghasilkan array kosong, bukan 404. */
    @GetMapping
    public List<MahasiswaResponse> list() {
        return service.findAll().stream().map(MahasiswaResponse::from).toList();
    }

    /**
     * Detail satu mahasiswa beserta mata kuliahnya, dari satu query JOIN.
     *
     * @throws com.togar.studentenrollment.exception.MahasiswaNotFoundException menjadi 404
     */
    @GetMapping("/{nim}")
    public MahasiswaDetailResponse detail(@PathVariable String nim) {
        return MahasiswaDetailResponse.from(service.findDetail(nim));
    }

    /**
     * Menyimpan mahasiswa baru.
     *
     * <p>Membalas 201 dengan header {@code Location} yang menunjuk sumber daya
     * yang baru dibuat, sehingga klien tidak perlu merangkai URL-nya sendiri.
     *
     * @throws com.togar.studentenrollment.exception.DuplicateNimException menjadi 409
     */
    @PostMapping
    public ResponseEntity<MahasiswaResponse> create(@Valid @RequestBody MahasiswaCreateRequest request) {
        var mahasiswa = request.toMahasiswa();
        service.create(mahasiswa);

        URI location = UriComponentsBuilder.fromPath("/api/students/{nim}")
                .buildAndExpand(mahasiswa.nim())
                .toUri();
        return ResponseEntity.created(location).body(MahasiswaResponse.from(mahasiswa));
    }

    /**
     * Memperbarui nama, angkatan, dan gender.
     *
     * <p>NIM diambil dari path dan tidak pernah dari body, sama seperti sisi MVC:
     * ia primary key sekaligus identitas, dan Oracle tidak punya
     * {@code ON UPDATE CASCADE}. Bentuk {@link MahasiswaUpdateRequest} memang tidak
     * menyediakan tempat untuk mengirimkannya.
     *
     * @throws com.togar.studentenrollment.exception.MahasiswaNotFoundException menjadi 404
     */
    @PutMapping("/{nim}")
    public MahasiswaResponse update(@PathVariable String nim,
                                    @Valid @RequestBody MahasiswaUpdateRequest request) {
        var mahasiswa = request.toMahasiswa(nim);
        service.update(nim, mahasiswa);
        return MahasiswaResponse.from(mahasiswa);
    }

    /**
     * Menghapus mahasiswa. Baris IRS miliknya ikut terhapus lewat
     * {@code ON DELETE CASCADE}.
     *
     * <p>Membalas 204 tanpa body, karena tidak ada lagi yang bisa dikembalikan.
     *
     * @throws com.togar.studentenrollment.exception.MahasiswaNotFoundException menjadi 404
     */
    @DeleteMapping("/{nim}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String nim) {
        service.delete(nim);
    }
}
