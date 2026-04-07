package ch.blw.agate.common.dto;

/**
 * Lists generic exception categories across the system. It standardizes error reporting for internal and client-facing responses.
 *
 * @CommentLastReviewed 2025-08-25
 */
public enum ExceptionEnum {
  GENERIC,
  EXTERNAL_SERVICE_ERROR,
  MAINTENANCE // Used by the AWS load balancer to indicate that the service is temporarily unavailable due to maintenance
}
