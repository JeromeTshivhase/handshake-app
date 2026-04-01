package com.trustmarket.handshake.shared.upload;

import com.trustmarket.handshake.shared.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.base-url}")
    private String baseUrl;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    /**
     * Store a single image file and return its public URL.
     */
    public String store(MultipartFile file) {
        validate(file);
        String filename = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
        Path target = Paths.get(uploadDir).resolve(filename).normalize();
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
        return baseUrl.replaceAll("/$", "") + "/" + filename;
    }

    /**
     * Store multiple image files and return their public URLs.
     */
    public List<String> storeAll(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("No files provided");
        }
        if (files.length > 10) {
            throw new BadRequestException("Maximum 10 images per upload");
        }
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(store(file));
        }
        return urls;
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BadRequestException("File exceeds maximum size of 10 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Only JPEG, PNG, WebP and GIF images are allowed");
        }
    }

    private String extension(String filename) {
        if (filename == null || !filename.contains(".")) return "jpg";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}


