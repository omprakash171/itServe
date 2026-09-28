package com.itserve.dto;

import java.util.List;

public record TicketActivityResponse(
        List<HistoryResponse> history,
        List<CommentResponse> comments
) {
}
