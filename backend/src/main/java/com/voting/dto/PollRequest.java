package com.voting.dto;

import lombok.Data;
import java.util.List;

@Data
public class PollRequest {
    private String title;
    private String description;
    private List<String> options;
    private Long duration;  // 秒
    private String txHash;
}
