package com.campusmate.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class UploadExceptionHandler {
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String oversizedUpload(RedirectAttributes redirect) {
        redirect.addFlashAttribute("uploadError", "File exceeds the configured upload limit.");
        return "redirect:/faculty/resources/new";
    }
}
