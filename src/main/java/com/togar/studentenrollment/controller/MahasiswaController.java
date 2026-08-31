package com.togar.studentenrollment.controller;

import com.togar.studentenrollment.dto.MahasiswaDetail;
import com.togar.studentenrollment.dto.MahasiswaForm;
import com.togar.studentenrollment.exception.DuplicateNimException;
import com.togar.studentenrollment.service.MahasiswaService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Route dan interaksi HTTP untuk pengelolaan mahasiswa.
 *
 * <p>Semua operasi yang mengubah data memakai POST — termasuk penghapusan —
 * sehingga tidak bisa terpicu oleh sekadar membuka URL atau oleh prefetch
 * peramban.
 */
@Controller
public class MahasiswaController {

    private final MahasiswaService service;

    public MahasiswaController(MahasiswaService service) {
        this.service = service;
    }

    /** Daftar mahasiswa. */
    @GetMapping("/")
    public String list(Model model) {
        model.addAttribute("students", service.findAll());
        return "students/list";
    }

    /** Form tambah. */
    @GetMapping("/students/new")
    public String newForm(Model model) {
        model.addAttribute("form", new MahasiswaForm());
        model.addAttribute("mode", "new");
        return "students/form";
    }

    /** Menyimpan mahasiswa baru. */
    @PostMapping("/students")
    public String create(@Valid @ModelAttribute("form") MahasiswaForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {

        if (binding.hasErrors()) {
            model.addAttribute("mode", "new");
            return "students/form";
        }

        try {
            service.create(form.toMahasiswa());
        } catch (DuplicateNimException e) {
            // NIM ganda bukan kesalahan sistem, melainkan isian yang perlu
            // diperbaiki -- tampilkan sebagai error field, bukan halaman error.
            binding.rejectValue("nim", "duplicate", "NIM " + e.getNim() + " sudah terdaftar");
            model.addAttribute("mode", "new");
            return "students/form";
        }

        redirect.addFlashAttribute("flashSuccess",
                "Mahasiswa " + form.getNama() + " berhasil ditambahkan.");
        return "redirect:/";
    }

    /** Detail mahasiswa beserta mata kuliah yang diambil (hasil satu query JOIN). */
    @GetMapping("/students/{nim}")
    public String detail(@PathVariable String nim, Model model) {
        MahasiswaDetail detail = service.findDetail(nim);
        model.addAttribute("detail", detail);
        model.addAttribute("mahasiswa", detail.mahasiswa());
        model.addAttribute("courses", detail.courses());
        return "students/detail";
    }

    /** Form edit. */
    @GetMapping("/students/{nim}/edit")
    public String editForm(@PathVariable String nim, Model model) {
        model.addAttribute("form", MahasiswaForm.from(service.findByNim(nim)));
        model.addAttribute("mode", "edit");
        model.addAttribute("originalNim", nim);
        return "students/form";
    }

    /** Menyimpan perubahan. */
    @PostMapping("/students/{nim}/edit")
    public String update(@PathVariable String nim,
                         @Valid @ModelAttribute("form") MahasiswaForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {

        // NIM tidak bisa diubah lewat edit. Form-nya sudah readonly, tapi
        // request bisa dipalsukan -- jadi NIM dari path yang dipakai, bukan
        // dari body. Dengan begini update tidak akan pernah menyentuh primary
        // key, dan cabang penolakan NIM ganda di service tetap ada sebagai
        // pertahanan lapis bawah bila pemanggil lain berperilaku beda.
        form.setNim(nim);

        if (binding.hasErrors()) {
            model.addAttribute("mode", "edit");
            model.addAttribute("originalNim", nim);
            return "students/form";
        }

        try {
            service.update(nim, form.toMahasiswa());
        } catch (DuplicateNimException e) {
            binding.rejectValue("nim", "duplicate", "NIM " + e.getNim() + " sudah dipakai mahasiswa lain");
            model.addAttribute("mode", "edit");
            model.addAttribute("originalNim", nim);
            return "students/form";
        }

        redirect.addFlashAttribute("flashSuccess",
                "Data " + form.getNama() + " berhasil diperbarui.");
        return "redirect:/";
    }

    /**
     * Menghapus mahasiswa.
     *
     * <p>Hanya menerima POST. Membuka URL ini lewat GET menghasilkan
     * 405 Method Not Allowed, bukan penghapusan data.
     */
    @PostMapping("/students/{nim}/delete")
    public String delete(@PathVariable String nim, RedirectAttributes redirect) {
        service.delete(nim);
        redirect.addFlashAttribute("flashSuccess",
                "Mahasiswa dengan NIM " + nim + " berhasil dihapus.");
        return "redirect:/";
    }
}
