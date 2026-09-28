package com.itserve.dto;

import com.itserve.entity.Attachment;

import java.time.Instant;

public record AttachmentResponse(
        Long id,
        String fileName,
        String contentType,
        long sizeBytes,
        String uploadedBy,
        Instant createdAt
) {
    public static AttachmentResponse from(Attachment a) {
        return new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getSizeBytes(),
                a.getUploadedBy().getFullName(), a.getCreatedAt());
    }
}
