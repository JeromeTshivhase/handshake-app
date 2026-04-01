package com.trustmarket.handshake.shared.upload;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.trustmarket.handshake.shared.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final long MAX_BYTES = 10L * 1024 * 1024; // 10 MB

    private final Cloudinary cloudinary;

    @Value("${cloudinary.folder:handshake-app/uploads}")
    private String cloudinaryFolder;

    public FileStorageService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * Store a single image file and return its public URL.
     */
    public String store(MultipartFile file) {
        return storeImage(file).getUrl();
    }

    public UploadedImage storeImage(MultipartFile file) {
        validate(file);
        validateCloudinaryConfig();

        try {
            Map<String, Object> uploadOptions = new HashMap<>();
            if (cloudinaryFolder != null && !cloudinaryFolder.isBlank()) {
                uploadOptions.put("folder", cloudinaryFolder);
            }

            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadOptions);
            String url = extractUrl(uploadResult);
            Object publicId = uploadResult.get("public_id");
            if (publicId == null || publicId.toString().isBlank()) {
                throw new RuntimeException("Cloudinary upload succeeded but no public_id was returned");
            }
            return UploadedImage.builder()
                    .url(url)
                    .publicId(publicId.toString())
                    .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Store multiple image files and return their public URLs.
     */
    public List<String> storeAll(MultipartFile[] files) {
        return storeAllImages(files).stream().map(UploadedImage::getUrl).toList();
    }

    public List<UploadedImage> storeAllImages(MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BadRequestException("No files provided");
        }
        if (files.length > 10) {
            throw new BadRequestException("Maximum 10 images per upload");
        }
        List<UploadedImage> uploads = new ArrayList<>();
        for (MultipartFile file : files) {
            uploads.add(storeImage(file));
        }
        return uploads;
    }

    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new BadRequestException("publicId is required");
        }
        validateCloudinaryConfig();

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete image from Cloudinary: " + e.getMessage(), e);
        }
    }

    // helpers

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

    private void validateCloudinaryConfig() {
        var config = cloudinary.config;
        if (isBlank(config.cloudName) || isBlank(config.apiKey) || isBlank(config.apiSecret)) {
            throw new BadRequestException("Cloudinary is not configured. Set CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY and CLOUDINARY_API_SECRET");
        }
    }

    private String extractUrl(Map<?, ?> uploadResult) {
        Object secureUrl = uploadResult.get("secure_url");
        if (secureUrl != null) {
            return secureUrl.toString();
        }

        Object url = uploadResult.get("url");
        if (url != null) {
            return url.toString();
        }
        throw new RuntimeException("Cloudinary upload succeeded but no URL was returned");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}


