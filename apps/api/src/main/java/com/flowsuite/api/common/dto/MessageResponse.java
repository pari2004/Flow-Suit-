package com.flowsuite.api.common.dto;

/** A minimal success envelope for endpoints that have nothing else to return. */
public record MessageResponse(String message) {
}
