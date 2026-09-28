package com.itserve.dto;

public record LoginResponse(String token, UserResponse user) {
}
