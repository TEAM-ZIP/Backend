package com.capstone.bszip.dto;

import lombok.Builder;
import lombok.Data;


@Data
@Builder
public class SuccessResponse<T> {
    private boolean result;
    private Integer status;
    private String message;
    private T data;
}
