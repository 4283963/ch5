package com.conference.mic.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

@Data
public class SpeakRequestDTO {

    @NotBlank(message = "座位号不能为空")
    private String seatNo;
}
