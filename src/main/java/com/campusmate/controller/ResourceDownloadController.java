package com.campusmate.controller;

import com.campusmate.model.Resource;
import com.campusmate.model.User;
import com.campusmate.service.ResourceService;
import com.campusmate.service.UserService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class ResourceDownloadController {
    private final ResourceService resourceService;
    private final UserService userService;
    public ResourceDownloadController(ResourceService resourceService, UserService userService) {
        this.resourceService = resourceService;
        this.userService = userService;
    }

    @GetMapping("/resources/{id}/download")
    public ResponseEntity<FileSystemResource> download(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found."));
        Resource resource = resourceService.getAuthorizedFile(id, user);
        Path path = resourceService.resolveStoredFile(resource.getStoredFilename());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource file not found.");
        String type = resource.getContentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : resource.getContentType();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(type));
        headers.setContentLength(resource.getFileSize());
        headers.setContentDisposition(ContentDisposition.attachment().filename(resource.getOriginalFilename()).build());
        headers.set("X-Content-Type-Options", "nosniff");
        return ResponseEntity.ok().headers(headers).body(new FileSystemResource(path));
    }
}
