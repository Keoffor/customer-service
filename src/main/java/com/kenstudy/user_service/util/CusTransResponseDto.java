package com.kenstudy.user_service.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CusTransResponseDto {
    private Integer transactionId;
    private Double amount;
    private String transactionType;
    private String transferBy;
    private String receiver;
    private String description;
    private LocalDate createdDated;
}
