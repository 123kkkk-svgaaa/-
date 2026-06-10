package com.voting.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String address;
    private String signature;
}
