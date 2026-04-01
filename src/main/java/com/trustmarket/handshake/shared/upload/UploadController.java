package com.trustmarket.handshake.shared.upload;

import com.trustmarket.handshake.shared.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Uploads", description = "Image upload endpoints")
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    /**
     * Upload a single image.
     * Returns a hosted Cloudinary URL in the response payload.
     */
    @Operation(summary = "Upload one image", operationId = "uploadSingleImage")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UploadResponse> uploadImage(@RequestParam("file") MultipartFile file) {
        UploadedImage uploadedImage = fileStorageService.storeImage(file);
        return ApiResponse.ok(UploadResponse.builder()
                .url(uploadedImage.getUrl())
                .publicId(uploadedImage.getPublicId())
                .build());
    }

    /**
     * Upload up to 10 images at once.
     * Returns hosted Cloudinary URLs in the response payload.
     *
     * <p><b>Note:</b> Maximum 10 images per upload. Exceeding this will result in a 400 error.</p>
     */
    @Operation(summary = "Upload multiple images (max 10)", description = "Upload up to 10 images at once. Maximum 10 images per upload. Exceeding this will result in a 400 error.", operationId = "uploadMultipleImages")
    @PostMapping(value = "/images/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<UploadResponse> uploadImages(@RequestPart("files") MultipartFile[] files) {
        var uploadedImages = fileStorageService.storeAllImages(files);
        return ApiResponse.ok(UploadResponse.builder()
                .urls(uploadedImages.stream().map(UploadedImage::getUrl).toList())
                .files(uploadedImages)
                .build());
    }

    @Operation(summary = "Delete image by Cloudinary public_id", operationId = "deleteImageByPublicId")
    @DeleteMapping(value = "/images", produces = MediaType.APPLICATION_JSON_VALUE)
    public ApiResponse<Void> deleteImage(@RequestParam("publicId") String publicId) {
        fileStorageService.deleteByPublicId(publicId);
        return ApiResponse.ok(null);
    }
}
