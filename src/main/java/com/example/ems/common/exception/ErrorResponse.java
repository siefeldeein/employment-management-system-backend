package com.example.ems.common.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ErrorResponse {

    private int status;
    private String message;
    private LocalDateTime timeStamp;
    private String path;                // Which endpoint fails
    private Map<String,String> errors;  // field-level validation errors
}
