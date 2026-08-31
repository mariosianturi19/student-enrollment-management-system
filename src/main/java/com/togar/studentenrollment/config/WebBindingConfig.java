package com.togar.studentenrollment.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * Merapikan input teks sebelum validasi berjalan, untuk seluruh controller.
 *
 * <p>{@code StringTrimmerEditor(true)} memangkas spasi di kedua ujung dan
 * mengubah string yang tersisa kosong menjadi {@code null}. Tanpa ini, isian
 * berupa spasi saja ({@code "   "}) akan lolos {@code @NotBlank} pada sebagian
 * skenario binding dan masuk ke database sebagai nama kosong.
 */
@Configuration
@ControllerAdvice
public class WebBindingConfig {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
