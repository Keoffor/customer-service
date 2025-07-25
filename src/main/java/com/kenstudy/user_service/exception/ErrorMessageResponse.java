package com.kenstudy.user_service.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;



@NoArgsConstructor
@Data

public class ErrorMessageResponse {
    private int status;
    private String error;
    private String message;
    private String path;

    public ErrorMessageResponse(HttpStatus status, String message, String path) {
        this.status = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.path = path;
    }

}
