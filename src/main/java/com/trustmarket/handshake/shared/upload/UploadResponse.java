package com.trustmarket.handshake.shared.upload;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UploadResponse {
    /** Single-file upload result */
    private String url;

    /** Single-file Cloudinary public id */
    private String publicId;

    /** Batch upload result with Cloudinary metadata */
    private List<UploadedImage> files;

    /** Batch upload result */
    private List<String> urls;
}

