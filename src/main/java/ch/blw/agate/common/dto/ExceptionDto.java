package ch.blw.agate.common.dto;

import lombok.Builder;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Encapsulates exception details for system-level errors. It provides a transport format for communicating messages, trace IDs, types,
 * and debug information.
 *
 * @CommentLastReviewed 2025-08-25
 */

@Builder
@Schema(name = "ExceptionDto")
public record ExceptionDto(
    String message,
    String traceId,
    ExceptionEnum type,
    String debugMessage
) {
}
