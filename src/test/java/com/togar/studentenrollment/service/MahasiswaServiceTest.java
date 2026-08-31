package com.togar.studentenrollment.service;

import com.togar.studentenrollment.dto.EnrolledCourse;
import com.togar.studentenrollment.dto.MahasiswaDetail;
import com.togar.studentenrollment.exception.DuplicateNimException;
import com.togar.studentenrollment.exception.MahasiswaNotFoundException;
import com.togar.studentenrollment.model.Mahasiswa;
import com.togar.studentenrollment.repository.MahasiswaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Menguji aturan bisnis {@link MahasiswaService} dengan repository tiruan.
 *
 * <p>Mockito murni, tanpa konteks Spring dan tanpa DataSource — sehingga
 * kelas ini berjalan tanpa Oracle sama sekali.
 */
@ExtendWith(MockitoExtension.class)
class MahasiswaServiceTest {

    @Mock
    private MahasiswaRepository repository;

    @InjectMocks
    private MahasiswaService service;

    private static final Mahasiswa SEKAR =
            new Mahasiswa("24060122001", "Sekar Ayu Pramesti", 2022, "P");

    // ------------------------------------------------------------ create

    @Test
    @DisplayName("create menolak NIM yang sudah terdaftar dan tidak menyentuh insert")
    void create_nimSudahAda_lemparDuplicate() {
        when(repository.existsByNim("24060122001")).thenReturn(true);

        assertThatThrownBy(() -> service.create(SEKAR))
                .isInstanceOf(DuplicateNimException.class)
                .hasMessageContaining("24060122001");

        verify(repository, never()).insert(any());
    }

    @Test
    @DisplayName("create dengan NIM baru meneruskan data ke repository")
    void create_nimBaru_panggilInsert() {
        when(repository.existsByNim("24060122001")).thenReturn(false);

        service.create(SEKAR);

        verify(repository).insert(SEKAR);
    }

    // ------------------------------------------------------------ update

    @Test
    @DisplayName("update menolak NIM asal yang tidak ada di database")
    void update_nimAsalTidakAda_lemparNotFound() {
        when(repository.existsByNim("99999999")).thenReturn(false);

        assertThatThrownBy(() -> service.update("99999999", SEKAR))
                .isInstanceOf(MahasiswaNotFoundException.class);

        verify(repository, never()).update(anyString(), any());
    }

    @Test
    @DisplayName("update menolak perubahan NIM ke nilai milik mahasiswa lain")
    void update_nimBaruSudahDipakai_lemparDuplicate() {
        Mahasiswa denganNimBaru = new Mahasiswa("24060122002", "Sekar Ayu Pramesti", 2022, "P");
        when(repository.existsByNim("24060122001")).thenReturn(true);
        when(repository.existsByNim("24060122002")).thenReturn(true);

        assertThatThrownBy(() -> service.update("24060122001", denganNimBaru))
                .isInstanceOf(DuplicateNimException.class)
                .hasMessageContaining("24060122002");

        verify(repository, never()).update(anyString(), any());
    }

    @Test
    @DisplayName("update tanpa mengubah NIM tidak dianggap duplikat terhadap dirinya sendiri")
    void update_nimTidakBerubah_berhasil() {
        Mahasiswa namaDiperbarui = new Mahasiswa("24060122001", "Sekar Ayu P.", 2022, "P");
        when(repository.existsByNim("24060122001")).thenReturn(true);

        service.update("24060122001", namaDiperbarui);

        verify(repository).update("24060122001", namaDiperbarui);
    }

    // ------------------------------------------------------------ delete

    @Test
    @DisplayName("delete melempar NotFound saat tidak ada baris yang terhapus")
    void delete_tidakAdaBarisTerhapus_lemparNotFound() {
        when(repository.deleteByNim("99999999")).thenReturn(0);

        assertThatThrownBy(() -> service.delete("99999999"))
                .isInstanceOf(MahasiswaNotFoundException.class);
    }

    @Test
    @DisplayName("delete berhasil saat satu baris terhapus")
    void delete_satuBarisTerhapus_berhasil() {
        when(repository.deleteByNim("24060122001")).thenReturn(1);

        service.delete("24060122001");

        verify(repository).deleteByNim("24060122001");
    }

    // ------------------------------------------------------------ query

    @Test
    @DisplayName("findDetail melempar NotFound saat repository mengembalikan Optional kosong")
    void findDetail_tidakAda_lemparNotFound() {
        when(repository.findDetailByNim("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findDetail("99999999"))
                .isInstanceOf(MahasiswaNotFoundException.class)
                .hasMessageContaining("99999999");
    }

    @Test
    @DisplayName("findDetail meneruskan hasil JOIN apa adanya, termasuk total SKS")
    void findDetail_ada_kembalikanDetail() {
        MahasiswaDetail detail = new MahasiswaDetail(SEKAR, List.of(
                new EnrolledCourse(1001, "IF2101", "Sistem Basis Data", 3, "Senin", "aktif"),
                new EnrolledCourse(1002, "IF2102", "PBO", 3, "Selasa", "lulus")));
        when(repository.findDetailByNim("24060122001")).thenReturn(Optional.of(detail));

        MahasiswaDetail hasil = service.findDetail("24060122001");

        assertThat(hasil.courses()).hasSize(2);
        assertThat(hasil.totalSks()).isEqualTo(6);
        assertThat(hasil.hasCourses()).isTrue();
    }

    @Test
    @DisplayName("findDetail mahasiswa tanpa IRS mengembalikan daftar kosong, bukan error")
    void findDetail_tanpaIrs_daftarKosong() {
        MahasiswaDetail detail = new MahasiswaDetail(SEKAR, List.of());
        when(repository.findDetailByNim("24060122001")).thenReturn(Optional.of(detail));

        MahasiswaDetail hasil = service.findDetail("24060122001");

        assertThat(hasil.hasCourses()).isFalse();
        assertThat(hasil.totalSks()).isZero();
    }

    @Test
    @DisplayName("findByNim melempar NotFound saat NIM tidak ada")
    void findByNim_tidakAda_lemparNotFound() {
        when(repository.findByNim("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByNim("99999999"))
                .isInstanceOf(MahasiswaNotFoundException.class);
    }

    @Test
    @DisplayName("findAll meneruskan daftar dari repository")
    void findAll_kembalikanDaftar() {
        when(repository.findAll()).thenReturn(List.of(SEKAR));

        assertThat(service.findAll()).containsExactly(SEKAR);
    }

    @Test
    @DisplayName("findAll pada database kosong mengembalikan daftar kosong, bukan null")
    void findAll_kosong_kembalikanDaftarKosong() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.findAll()).isEmpty();
    }

    // ------------------------------------------------- perilaku model/DTO

    @Test
    @DisplayName("genderLabel menerjemahkan L/P dan menangani nilai tak dikenal")
    void genderLabel_terjemahkanDenganBenar() {
        assertThat(new Mahasiswa("1", "A", 2022, "L").genderLabel()).isEqualTo("Laki-laki");
        assertThat(new Mahasiswa("1", "A", 2022, "P").genderLabel()).isEqualTo("Perempuan");
        assertThat(new Mahasiswa("1", "A", 2022, "X").genderLabel()).isEqualTo("-");
        assertThat(new Mahasiswa("1", "A", 2022, null).genderLabel()).isEqualTo("-");
    }

    @Test
    @DisplayName("statusBadgeClass memetakan tiap status ke kelas CSS-nya")
    void statusBadgeClass_petakanStatus() {
        assertThat(course("lulus").statusBadgeClass()).isEqualTo("badge-status badge-lulus");
        assertThat(course("aktif").statusBadgeClass()).isEqualTo("badge-status badge-aktif");
        assertThat(course("gagal").statusBadgeClass()).isEqualTo("badge-status badge-gagal");
        // Status di luar tiga yang dikenal tetap tampil, dengan gaya netral
        assertThat(course("cuti").statusBadgeClass()).isEqualTo("badge-status badge-netral");
        assertThat(course(null).statusBadgeClass()).isEqualTo("badge-status badge-netral");
    }

    @Test
    @DisplayName("MahasiswaDetail mengubah courses null menjadi daftar kosong")
    void mahasiswaDetail_coursesNull_jadiDaftarKosong() {
        MahasiswaDetail detail = new MahasiswaDetail(SEKAR, null);

        assertThat(detail.courses()).isEmpty();
        assertThat(detail.hasCourses()).isFalse();
    }

    private static EnrolledCourse course(String status) {
        return new EnrolledCourse(1, "IF2101", "Sistem Basis Data", 3, "Senin", status);
    }
}
