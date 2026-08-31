/*
 * Konfirmasi sebelum menghapus.
 *
 * Ditulis sebagai file terpisah, bukan atribut onsubmit di dalam HTML, karena:
 *   - Thymeleaf 3.1 membatasi ekspresi di dalam atribut event demi keamanan;
 *   - nama mahasiswa dibaca dari atribut data-*, sehingga tanda kutip atau
 *     karakter khusus di dalam nama tidak bisa merusak kode JavaScript.
 *
 * Bila JavaScript mati, form tetap berfungsi -- hanya kehilangan dialog
 * konfirmasinya. Penghapusan tetap butuh POST.
 */
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm(form.getAttribute('data-confirm'))) {
                event.preventDefault();
            }
        });
    });
});
