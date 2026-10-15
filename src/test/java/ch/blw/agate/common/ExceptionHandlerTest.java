package ch.blw.agate.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.blw.agate.common.dto.ExceptionDto;
import ch.blw.agate.common.dto.ExceptionEnum;
import ch.blw.agate.common.dto.ExternalServiceExceptionDto;
import ch.blw.agate.common.exceptions.ExternalWebServiceException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class ExceptionHandlerTest {

  private static final String TEST_TRACE_ID = "00000000000000000000000000000abc";

  private ExceptionHandler exceptionHandler;
  private Scope spanScope;

  @BeforeEach
  void setUp() {
    exceptionHandler = new ExceptionHandler();
    // Activate a span with a known trace id so ExceptionHandler#currentTraceId() has something to read.
    SpanContext spanContext = SpanContext.create(
        TEST_TRACE_ID, "0000000000000abc", TraceFlags.getSampled(), TraceState.getDefault());
    spanScope = Span.wrap(spanContext).makeCurrent();
  }

  @AfterEach
  void tearDown() {
    spanScope.close();
  }

  @ParameterizedTest(name = "handleAll, debug={0}")
  @ValueSource(booleans = {false, true})
  void handleAll(boolean debug) {
    exceptionHandler.returnDebug = debug;
    RuntimeException ex = new RuntimeException("something went wrong");

    Response response = exceptionHandler.handleAll(ex);
    ExceptionDto dto = (ExceptionDto) response.getEntity();

    assertThat(response.getStatus())
        .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    assertThat(dto.message()).isEqualTo("An error occurred");
    assertThat(dto.type()).isEqualTo(ExceptionEnum.GENERIC);
    assertThat(dto.traceId()).isEqualTo(TEST_TRACE_ID);

    if (debug) {
      assertThat(dto.debugMessage()).isEqualTo("something went wrong");
    } else {
      assertThat(dto.debugMessage()).isNull();
    }
  }

  @ParameterizedTest(name = "handleConstraintViolation, debug={0}")
  @ValueSource(booleans = {false, true})
  void handleConstraintViolation(boolean debug) {
    exceptionHandler.returnDebug = debug;

    ConstraintViolation<?> violation01 =
        mockConstraintViolation("test-property-01", "must not be null");
    ConstraintViolation<?> violation02 =
        mockConstraintViolation("test-property-02", "must be greater than 0");
    ConstraintViolationException ex =
        new ConstraintViolationException(Set.of(violation01, violation02));

    Response response = exceptionHandler.handleConstraintViolation(ex);
    ExceptionDto dto = (ExceptionDto) response.getEntity();

    assertThat(response.getStatus())
        .isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(dto.message()).isEqualTo("Validation failed");
    assertThat(dto.type()).isEqualTo(ExceptionEnum.GENERIC);

    if (debug) {
      assertThat(dto.debugMessage()).isEqualTo(
          "test-property-01: must not be null, test-property-02: must be greater than 0");
    } else {
      assertThat(dto.debugMessage()).isNull();
    }
  }

  @ParameterizedTest(name = "handleNotFound, debug={0}")
  @ValueSource(booleans = {false, true})
  void handleNotFound(boolean debug) {
    exceptionHandler.returnDebug = debug;
    NotFoundException exception = new NotFoundException("object not found");

    Response response = exceptionHandler.handleNotFound(exception);
    ExceptionDto dto = (ExceptionDto) response.getEntity();

    assertThat(response.getStatus())
        .isEqualTo(Response.Status.NOT_FOUND.getStatusCode());
    assertThat(dto.message()).isEqualTo("NOT_FOUND");
    assertThat(dto.type()).isEqualTo(ExceptionEnum.GENERIC);
    assertThat(dto.traceId()).isEqualTo(TEST_TRACE_ID);

    if (debug) {
      assertThat(dto.debugMessage()).isEqualTo("object not found");
    } else {
      assertThat(dto.debugMessage()).isNull();
    }
  }

  @ParameterizedTest(name = "handleWebAppException, debug={0}")
  @ValueSource(booleans = {false, true})
  void handleWebAppException(boolean debug) {
    exceptionHandler.returnDebug = debug;
    WebApplicationException exception = new WebApplicationException("access denied");

    Response response = exceptionHandler.handleWebAppException(exception);
    ExceptionDto dto = (ExceptionDto) response.getEntity();

    assertThat(response.getStatus())
        .isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    assertThat(dto.message()).isEqualTo("Web exception");
    assertThat(dto.type()).isEqualTo(ExceptionEnum.GENERIC);
    assertThat(dto.traceId()).isEqualTo(TEST_TRACE_ID);

    if (debug) {
      assertThat(dto.debugMessage()).isEqualTo("access denied");
    } else {
      assertThat(dto.debugMessage()).isNull();
    }
  }

  static Stream<Arguments> badRequestHandlers() {
    return Stream.of(
        Arguments.of("ValidationException", (Function<ExceptionHandler, Response>)
            h -> h.handleValidationException(new ValidationException("invalid"))),
        Arguments.of("MismatchedInputException", (Function<ExceptionHandler, Response>)
            h -> h.handleValidationException(MismatchedInputException.from(null, String.class, "invalid"))),
        Arguments.of("IllegalArgumentException", (Function<ExceptionHandler, Response>)
            h -> h.handleIllegalArgumentException(new IllegalArgumentException("invalid"))),
        Arguments.of("IllegalStateException", (Function<ExceptionHandler, Response>)
            h -> h.handleIllegalStateException(new IllegalStateException("invalid"))));
  }

  @ParameterizedTest(name = "{0} -> 400")
  @MethodSource("badRequestHandlers")
  void badRequestHandlers(String name, Function<ExceptionHandler, Response> handler) {
    exceptionHandler.returnDebug = true;

    Response response = handler.apply(exceptionHandler);
    ExceptionDto dto = (ExceptionDto) response.getEntity();

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(dto.message()).isEqualTo("An error occurred");
    assertThat(dto.debugMessage()).startsWith("invalid");
    assertThat(dto.traceId()).isEqualTo(TEST_TRACE_ID);
  }

  @ParameterizedTest(name = "handleExternalWebServiceException, debug={0}")
  @ValueSource(booleans = {false, true})
  void handleExternalWebServiceException(boolean debug) {
    exceptionHandler.returnDebug = debug;

    Response response = exceptionHandler.handleExternalWebServiceException(
        new ExternalWebServiceException("soap call failed"));
    ExternalServiceExceptionDto dto = (ExternalServiceExceptionDto) response.getEntity();

    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_GATEWAY.getStatusCode());
    assertThat(dto.message()).isEqualTo("External service failed");
    assertThat(dto.type()).isEqualTo(ExceptionEnum.EXTERNAL_SERVICE_ERROR);
    assertThat(dto.traceId()).isEqualTo(TEST_TRACE_ID);
    assertThat(dto.debugMessage()).isEqualTo(debug ? "soap call failed" : null);
  }

  @Test
  void givenNoActiveSpan_thenTraceIdIsNull() {
    spanScope.close();
    // An empty context has no valid span, so no trace id can be returned.
    spanScope = Context.root().makeCurrent();

    ExceptionDto dto = (ExceptionDto) exceptionHandler.handleAll(new RuntimeException("x")).getEntity();

    assertThat(dto.traceId()).isNull();
  }

  private ConstraintViolation<?> mockConstraintViolation(String path, String message) {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    Path propertyPath = mock(Path.class);
    when(violation.getPropertyPath()).thenReturn(propertyPath);
    when(propertyPath.toString()).thenReturn(path);
    when(violation.getMessage()).thenReturn(message);
    return violation;
  }
}
