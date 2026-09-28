package com.itserve.dto;

import java.util.List;

public record TicketDetailResponse(
        TicketResponse ticket,
        List<CommentResponse> comments,
        List<HistoryResponse> history
) {
}
