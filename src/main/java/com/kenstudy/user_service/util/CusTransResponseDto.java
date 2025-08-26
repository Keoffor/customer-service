package com.kenstudy.user_service.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CusTransResponseDto {
    private Integer transactId;
    private String transferEventId;
    private Double amount;
    private String transactionType;
    private String transferBy;
    private String receiver;
    private String description;
    private LocalDateTime createdDated;
    private String status;
    private String reason;
}
