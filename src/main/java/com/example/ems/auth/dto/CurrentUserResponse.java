package com.example.ems.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CurrentUserResponse {

    private String username;

    private String email;

    private List<String> roles;

    private Long employeeId;

}