package com.trustmarket.handshake.shared.upload;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadedImage {
    private String url;
    private String publicId;
}

