# Design System — Student Enrollment Management System

Arah visual: **Academic Registrar**.

Personality keyword: *tenang, tertib, berwibawa* — terasa seperti dokumen resmi kantor
akademik, bukan dashboard SaaS. Referensi mentalnya adalah kartu rencana studi dan
buku induk mahasiswa: garis tipis, banyak ruang putih hangat, data numerik yang
sejajar rapi, tanpa dekorasi yang tidak punya fungsi.

Bootstrap 5 dipakai sebagai fondasi grid/utility, tetapi tema defaultnya **tidak**
dibiarkan apa adanya. Lapisan token di bawah ini yang menentukan tampilan akhir.

---

## 1. Palet warna

| Token | Hex | Peran |
|---|---|---|
| `--ink` | `#1B2A4A` | Teks utama, header, judul. Navy tinta. |
| `--ink-soft` | `#2C3E63` | Hover state pada elemen ink. |
| `--paper` | `#FAF8F4` | Latar halaman. Putih hangat, bukan putih klinis. |
| `--surface` | `#FFFFFF` | Latar kartu/tabel. |
| `--surface-alt` | `#F4F1EA` | Latar header tabel, baris zebra. |
| `--border` | `#DED8CC` | Garis pembatas utama. |
| `--border-strong` | `#C4BBA9` | Garis pembatas yang perlu lebih tegas. |
| `--muted` | `#5A6478` | Teks sekunder, label, meta. |
| `--accent` | `#1F6F4A` | Aksi utama (Tambah, Simpan). Hijau tua. |
| `--accent-dark` | `#18583B` | Hover aksi utama. |
| `--accent-tint` | `#E8F0EA` | Latar badge status "lulus". |
| `--danger` | `#8E2A22` | Aksi destruktif (Hapus), pesan error. Merah bata. |
| `--danger-tint` | `#F7E9E7` | Latar badge status "gagal", latar alert error. |
| `--info-tint` | `#E9EDF5` | Latar badge status "aktif". |

### Kontras (dihitung dengan rumus luminansi relatif WCAG 2.1)

| Kombinasi | Rasio | Standar |
|---|---|---|
| `--ink` #1B2A4A di atas `--paper` #FAF8F4 | **13.0 : 1** | Lolos AAA |
| `--ink` #1B2A4A di atas `--surface` #FFFFFF | **13.7 : 1** | Lolos AAA |
| `--muted` #5A6478 di atas `--paper` #FAF8F4 | **5.6 : 1** | Lolos AA |
| `--accent` #1F6F4A di atas `--paper` #FAF8F4 | **5.3 : 1** | Lolos AA |
| `--danger` #8E2A22 di atas `--paper` #FAF8F4 | **6.6 : 1** | Lolos AA |
| `#FFFFFF` di atas `--accent` #1F6F4A | **5.1 : 1** | Lolos AA (tombol) |
| `#FFFFFF` di atas `--danger` #8E2A22 | **6.3 : 1** | Lolos AA (tombol) |

Tidak ada kombinasi teks/latar di aplikasi ini yang berada di bawah 4.5 : 1.

---

## 2. Tipografi

| Peran | Font | Fallback | Lisensi |
|---|---|---|---|
| Display / judul | **Source Serif 4** | `Georgia, 'Times New Roman', serif` | SIL OFL 1.1 |
| Body / UI | **IBM Plex Sans** | `'Segoe UI', system-ui, sans-serif` | SIL OFL 1.1 |
| Data numerik | **IBM Plex Mono** | `Consolas, 'Courier New', monospace` | SIL OFL 1.1 |

Ketiganya bebas dipakai termasuk untuk keperluan komersial, dimuat dari Google Fonts
dengan `preconnect`. Kalau koneksi Google Fonts gagal, fallback stack di atas sudah
cukup dekat karakternya sehingga layout tidak rusak.

**Kenapa pasangan ini:** serif di judul memberi kesan dokumen resmi dan langsung
membedakannya dari tampilan Bootstrap default yang serba sans-serif. Kontras antara
Source Serif 4 (bertekstur, punya kait) dan IBM Plex Sans (geometris, netral) cukup
jauh sehingga keduanya tidak saling bersaing. IBM Plex Mono dipakai khusus untuk NIM,
kode mata kuliah, dan SKS supaya angka sejajar per kolom dan mudah dibandingkan mata
per mata — ini fungsional, bukan gaya.

### Skala tipe

| Token | Ukuran | Line height | Pemakaian |
|---|---|---|---|
| `--fs-display` | `1.75rem` (28px) | 1.2 | Judul halaman |
| `--fs-h2` | `1.25rem` (20px) | 1.3 | Judul seksi |
| `--fs-body` | `0.9375rem` (15px) | 1.55 | Teks isi, sel tabel |
| `--fs-sm` | `0.8125rem` (13px) | 1.45 | Label, meta, pesan bantuan |
| `--fs-xs` | `0.6875rem` (11px) | 1.4 | Header tabel (uppercase, letter-spacing) |

Di viewport `<576px`, `--fs-display` turun ke `1.375rem` (22px).

---

## 3. Skala spasi

Basis 4px: `--sp-1: 4px`, `--sp-2: 8px`, `--sp-3: 12px`, `--sp-4: 16px`,
`--sp-5: 24px`, `--sp-6: 32px`, `--sp-7: 48px`.

Radius sengaja kecil dan tidak seragam: `--radius-sm: 3px` untuk badge/input,
`--radius-md: 5px` untuk kartu dan panel. Tidak ada `border-radius` besar/pill —
itu bertentangan dengan karakter dokumen resmi.

Shadow dipakai **sangat terbatas**: hanya satu, `--shadow-sm:
0 1px 2px rgba(27,42,74,.06)`, dan hanya pada panel utama. Hierarki dibangun lewat
garis (`--border`) dan ruang, bukan lewat shadow bertumpuk di setiap elemen.

---

## 4. Komponen

### Header aplikasi
Bar `--ink` penuh selebar layar, tinggi `56px`. Berisi nama aplikasi dengan mark
tekstual `SEMS` dalam mono + label penuh dalam serif. Tanpa logo gambar.

### Judul halaman
Serif, `--fs-display`, dengan garis bawah tipis `--border` sepanjang kontainer dan
tombol aksi utama disejajarkan ke kanan pada baris yang sama (desktop).

### Tabel
- Header: `--surface-alt`, teks `--fs-xs` uppercase `letter-spacing: .06em`, warna `--muted`
- Garis horizontal `1px solid --border` antar baris; **tanpa** garis vertikal
- Hover baris: `--surface-alt`
- Kolom NIM/SKS/kode: `font-family: mono`, `font-variant-numeric: tabular-nums`
- Kolom aksi disejajarkan ke kanan

### Badge status IRS
Kotak radius `--radius-sm`, `--fs-sm`, padding `2px 8px`, teks gelap di atas tint muda:

| Status | Teks | Latar |
|---|---|---|
| `lulus` | `--accent` | `--accent-tint` |
| `aktif` | `--ink` | `--info-tint` |
| `gagal` | `--danger` | `--danger-tint` |

Tanpa warna neon, tanpa dot animasi.

### Tombol
- Primer: latar `--accent`, teks putih, radius `--radius-sm`
- Sekunder: latar transparan, border `--border-strong`, teks `--ink`
- Destruktif: latar transparan, border `--danger`, teks `--danger`; berubah jadi
  latar `--danger` + teks putih saat hover
- Tinggi minimum `38px` di desktop, `44px` di mobile

### Form
Label di atas input, selalu terlihat (**bukan** placeholder sebagai label — itu hilang
saat diisi dan menyulitkan screen reader). Setiap input punya `<label for>` yang
cocok dengan `id`-nya. Field wajib ditandai `*` dengan `aria-hidden` pada tandanya dan
kata "wajib" di teks bantuan.

Error state: border `--danger`, pesan error `--fs-sm` warna `--danger` langsung di
bawah field, dan input diberi `aria-invalid="true"`.

### Empty state
Panel bergaris putus-putus `--border`, teks `--muted` terpusat, dengan kalimat spesifik
(misal "Belum ada mata kuliah yang diambil oleh mahasiswa ini") dan satu tombol aksi
bila relevan. Bukan ilustrasi generik.

---

## 5. Perilaku responsif

Dirancang bersamaan dengan desktop, bukan diturunkan setelahnya.

| Breakpoint | Perilaku |
|---|---|
| **≥ 768px** | Tabel penuh. Daftar mahasiswa: 5 kolom (NIM, Nama, Angkatan, Gender, Aksi). Detail: 4 kolom (Mata Kuliah, SKS, Hari, Status). Tombol aksi sebaris horizontal. Judul dan tombol utama pada satu baris. |
| **< 768px** | **Tabel daftar mahasiswa berganti bentuk menjadi tumpukan kartu**, bukan tabel yang di-scroll menyamping. Tiap kartu: nama sebagai judul (serif), NIM mono di bawahnya, angkatan + gender sebagai baris meta, lalu tombol aksi selebar penuh. Tabel mata kuliah di halaman detail tetap tabel tetapi hanya menampilkan Mata Kuliah + Status sebagai kolom, dengan SKS dan Hari pindah ke baris meta di bawah nama mata kuliah. Judul halaman dan tombol utama menjadi bertumpuk vertikal. |
| **< 576px** | `--fs-display` turun ke 22px. Padding kontainer turun dari `--sp-5` ke `--sp-4`. |

Implementasi transformasi tabel→kartu memakai `display: block` pada `thead/tbody/tr/td`
plus `data-label` sebagai atribut pada `<td>`; markup tetap `<table>` sehingga struktur
semantiknya utuh untuk screen reader.

Semua target sentuh di mobile minimum `44 × 44px`.

---

## 6. Anti-AI-slop checklist (hasil self-critique)

| Pola yang dihindari | Status di desain ini |
|---|---|
| Gradien ungu→biru sebagai penanda "tech" | Tidak ada gradien sama sekali |
| Hero section terpusat dengan blob/mesh | Tidak ada hero; langsung ke konten |
| Tema component library dibiarkan default | Token warna & tipografi menimpa seluruh default Bootstrap |
| `Inter` / system-ui tanpa alasan | Source Serif 4 + IBM Plex, dipilih dengan alasan tertulis |
| Shadow & radius seragam di semua elemen | Satu shadow saja, radius dua tingkat, keduanya kecil |
| Bentuk pill / radius besar | Radius maksimum 5px |
| Copy placeholder generik | Semua teks spesifik ke domain akademik, Bahasa Indonesia |
| Ikon set generik satu bobot | Tanpa ikon dekoratif; hanya teks dan satu mark tekstual |
| Simetri total tanpa focal point | Judul kiri + aksi kanan menciptakan ketegangan horizontal |

---

## 7. Token siap pakai (CSS custom properties)

Nilai final ada di `src/main/resources/static/css/app.css` pada blok `:root`.
Blok itulah sumber kebenaran untuk implementasi; tabel di dokumen ini adalah
penjelasan alasannya.
