package com.trustmarket.handshake.shared.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final ObjectProvider<Cloudinary> cloudinaryProvider;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Value("${app.upload.base-url}")
    private String baseUrl;

    @Value("${cloudinary.url:}")
    private String cloudinaryUrl;

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Value("${cloudinary.folder:handshake-app/uploads}")
    private String cloudinaryFolder;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Paths.get(uploadDir));
    }

    /**
     * Store a single image file and return its public URL.
     */
    public String store(MultipartFile file) {
        return storeImage(file).getUrl();
    }

    /**
     * Store a single image and return URL plus optional Cloudinary publicId.
     */
    public UploadedImage storeImage(MultipartFile file) {
        validate(file);

        if (isCloudinaryConfigured()) {
            try {
                return storeWithCloudinary(file);
            } catch (Exception ex) {
                // Graceful fallback keeps uploads available during Cloudinary outages.
                log.warn("Cloudinary upload failed, falling back to local storage: {}", ex.getMessage());
            }
        }

        return storeLocally(file);
    }

    private UploadedImage storeWithCloudinary(MultipartFile file) throws IOException {
        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary bean is not available");
        }

        Map<?, ?> result = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", cloudinaryFolder,
                        "resource_type", "image",
                        "overwrite", false
                )
        );

        Object secureUrl = result.get("secure_url");
        Object publicId = result.get("public_id");
        if (secureUrl == null) {
            throw new IllegalStateException("Cloudinary response missing secure_url");
        }

        return UploadedImage.builder()
                .url(String.valueOf(secureUrl))
                .publicId(publicId == null ? null : String.valueOf(publicId))
                .build();
    }

    private UploadedImage storeLocally(MultipartFile file) {
        String filename = UUID.randomUUID() + "." + extension(file.getOriginalFilename());
        Path target = Paths.get(uploadDir).resolve(filename).normalize();
        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
        return UploadedImage.builder()
                .url(baseUrl.replaceAll("/$", "") + "/" + filename)
                .publicId(null)
                .build();
    }

    /**
     * Store multiple image files and return their public URLs.
     */
    public List<String> storeAll(MultipartFile[] files) {
        return storeAllImages(files).stream().map(UploadedImage::getUrl).collect(Collectors.toList());
    }

    /**
     * Store multiple images and return URL/publicId metadata.
     */
    public List<UploadedImage> storeAllImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("No files provided");
        }
        if (files.length > 10) {
            throw new BadRequestException("Maximum 10 images per upload");
        }
        List<UploadedImage> images = new ArrayList<>();
        for (MultipartFile file : files) {
            images.add(storeImage(file));
        }
        return images;
    }

    private boolean isCloudinaryConfigured() {
        if (cloudinaryUrl != null && !cloudinaryUrl.isBlank()) {
            return true;
        }
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
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


