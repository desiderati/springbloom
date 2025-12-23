/*
 * Copyright (c) 2025 - Felipe Desiderati
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package dev.springbloom.web.rest.exception;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.springbloom.core.exception.*;
import io.openapi.client.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.transaction.TransactionException;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.*;

@Validated
@ConditionalOnWebApplication
@ConditionalOnSingleCandidate(ExceptionHandlingController.class)
@ControllerAdvice(annotations = {RestController.class, RepositoryRestController.class})
public class ExceptionHandlingController extends ResponseEntityExceptionHandler implements MessageSourceAware {

    private static final Logger logNotSafe =
        LoggerFactory.getLogger(String.format("%sNotPrivacySafe", ExceptionHandlingController.class.getName()));

    private static final String STATUS_ATTR = "status";

    private static final String HTTP_ERROR_PREFIX = "http_error_";

    private static final String DEFAULT_ERROR_MESSAGE =
        "The server encountered an unexpected condition that prevented it from fulfilling the request!";

    private static final String DEFAULT_OPEN_API_ERROR_CODE =
        "OpenApi.message";

    private static final String DEFAULT_OPEN_API_ERROR_MESSAGE =
        "There was an error while accessing Open API!";

    private static final String DEFAULT_VALIDATION_ERROR_CODE =
        "DefaultValidation.message";

    private static final String DEFAULT_VALIDATION_ERROR_MESSAGE =
        "There was an error while validating the request parameters!";

    public static final HttpStatusCode DEFAULT_HTTP_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    private final ObjectMapper objectMapper;
    private final ModelMapper modelMapper;

    @Setter
    private MessageSource messageSource;

    private final Map<String, List<String>> shouldLogAsWarning;

    @Autowired
    public ExceptionHandlingController(
        @Value("${spring.web.exception-handler.should-log-as-warning}") @NotEmpty List<@NotBlank String> shouldLogAsWarning,
        ObjectMapper objectMapper,
        ModelMapper modelMapper
    ) {
        this.shouldLogAsWarning = new HashMap<>();
        shouldLogAsWarning.forEach(ex -> {
            String[] exParts = ex.split(":");
            try {
                List<String> msgsList = new ArrayList<>();
                if (exParts.length > 1) {
                    msgsList.addAll(Arrays.asList(exParts).subList(1, exParts.length));
                }

                var key = (exParts[0].equals("null")) ? "null" : Class.forName(exParts[0]).getName();
                this.shouldLogAsWarning.put(key, msgsList);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalStateException("Class not found for exception: " + exParts[0], cnfe);
            }
        });

        this.objectMapper = objectMapper;
        this.modelMapper = modelMapper;
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ResponseExceptionDTO> handleThrowable(
        HttpServletRequest request, Throwable ex
    ) {
        ResponseEntity<ResponseExceptionDTO> responseEntity = handleApiExceptionCopycat(request, ex);
        if (responseEntity != null) {
            return responseEntity;
        }

        Throwable rootCause = ExceptionUtils.getRootCause(ex);
        if (rootCause instanceof jakarta.validation.ConstraintViolationException) {
            return handleJavaxConstraintViolationException(
                request, (jakarta.validation.ConstraintViolationException) rootCause
            );
        }

        return handleHttpErrorException(request, DEFAULT_HTTP_STATUS, ex);
    }

    /**
     * Cases where the {@link ApiException} is in another package than the default.
     */
    private ResponseEntity<ResponseExceptionDTO> handleApiExceptionCopycat(HttpServletRequest request, Throwable ex) {
        if (ex instanceof ApiException) {
            ApiException apiException = modelMapper.map(ex, ApiException.class);
            if (apiException.getCode() != 0 && apiException.getResponseBody() != null
                && !apiException.getResponseHeaders().isEmpty()) {

                logNotSafe.debug("Transforming {} into {}", ex.getClass().getName(), ApiException.class.getName());
                return handleApiException(request, apiException);
            }
        }
        return null;
    }

    @ExceptionHandler(HttpStatusCodeException.class)
    public ResponseEntity<ResponseExceptionDTO> handleHttpStatusCodeException(
        HttpServletRequest request, HttpStatusCodeException ex
    ) {
        return handleOpenApiException(request, ex, ex.getStatusCode() + " " + ex.getStatusText(),
            ex.getResponseBodyAsString(), ex.getResponseHeaders());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ResponseExceptionDTO> handleApiException(HttpServletRequest request, ApiException ex) {
        String errorMessage =
            getMessage(request.getLocale(), DEFAULT_OPEN_API_ERROR_CODE, DEFAULT_OPEN_API_ERROR_MESSAGE);

        return handleOpenApiException(
            request, ex, errorMessage, ex.getResponseBody(), ex.getResponseHeaders()
        );
    }

    private ResponseEntity<ResponseExceptionDTO> handleOpenApiException(
        HttpServletRequest request, Exception exception,
        // We had to extract these parameters because the exceptions don't inherit the same parent class.
        String errorMessage, String responseBody, Map<String, List<String>> responseHeaders
    ) {
        StringBuilder remoteExceptionStr = new StringBuilder();
        if (responseHeaders != null) {
            remoteExceptionStr.append(System.lineSeparator()).append("Headers:").append(System.lineSeparator());
            for (final Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
                remoteExceptionStr.append("  ").append(entry.getKey()).append(": ")
                    .append(StringUtils.join(entry.getValue(), ", ")).append(System.lineSeparator());
            }
        }

        ValidationResponseExceptionDTO responseException = null;
        Map<String, Object> responseErrorAttributes = null;
        if (responseBody != null) {
            remoteExceptionStr.append("Body:").append(System.lineSeparator());
            responseException = deserializeApiResponseException(responseBody);
            if (!appendResponseStr(remoteExceptionStr, responseException)) {
                // As a fallback, we try to deserialize the response body into a map of objects.
                responseErrorAttributes = deserializeApiResponseErrorAttributes(responseBody);
                if (!appendResponseStr(remoteExceptionStr, responseErrorAttributes)) {
                    logNotSafe.warn("It is was not possible deserialize API using previous methods! " +
                        "Appending response body directly as String...");
                    remoteExceptionStr.append(responseBody);
                }
            }
        }

        // We cannot use instanceOf because if the ApiException body has at least one property equal to one of
        // the properties of the ResponseExceptionDTO class, the deserialized object will be valid.
        if (responseException != null && responseException.getType().equals(ResponseExceptionDTO.class.getName())) {
            return getResponseExceptionDTO(
                request, responseException.getMessage() + remoteExceptionStr, responseException
            );
        } else if (responseErrorAttributes != null) {
            return getResponseExceptionDTO(
                request, errorMessage + remoteExceptionStr, responseErrorAttributes
            );
        }

        // Regardless of the return status of the Open API request, we will generate the same standard error
        // for our clients.
        return handleThrowable(request, exception);
    }

    @NotNull
    private ResponseEntity<ResponseExceptionDTO> getResponseExceptionDTO(
        HttpServletRequest request, String remoteExceptionStr, ValidationResponseExceptionDTO responseException
    ) {
        if (DEFAULT_HTTP_STATUS.value() == responseException.getStatus()) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, remoteExceptionStr);
        }

        UUID uuid = logMessage(request, remoteExceptionStr);
        return ResponseEntity.status(responseException.getStatus()).body(
            new ValidationResponseExceptionDTO(
                uuid,
                responseException.getErrorCode(),
                responseException.getMessage(),
                responseException.getStatus(),
                responseException.getValidationMessages(),
                responseException.getArgs()
            )
        );
    }

    @NotNull
    private ResponseEntity<ResponseExceptionDTO> getResponseExceptionDTO(
        HttpServletRequest request, String remoteExceptionStr, Map<String, Object> responseErrorAttributes
    ) {
        if (DEFAULT_HTTP_STATUS.value() == (int) responseErrorAttributes.get(STATUS_ATTR)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, remoteExceptionStr);
        }

        String errorCode = HTTP_ERROR_PREFIX + responseErrorAttributes.get(STATUS_ATTR);
        String errorMsg = getMessage(request.getLocale(), errorCode, DEFAULT_ERROR_MESSAGE);
        UUID uuid = logMessage(request, remoteExceptionStr);
        return ResponseEntity.status((int) responseErrorAttributes.get(STATUS_ATTR)).body(
            new ResponseExceptionDTO(uuid, errorCode, errorMsg, (int) responseErrorAttributes.get(STATUS_ATTR))
        );
    }

    private ValidationResponseExceptionDTO deserializeApiResponseException(String responseBody) {
        try {
            TypeReference<ValidationResponseExceptionDTO> typeRef = new TypeReference<>() {
            };

            return objectMapper.readValue(responseBody, typeRef);
        } catch (IOException ex) {
            logNotSafe.info("It is was not possible deserialize API exception body to response exception! " +
                "Message: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * As fallback, we try to deserialize the response body into a map of objects ({@link ErrorAttributes}).
     *
     * @see DefaultErrorAttributes
     */
    private Map<String, Object> deserializeApiResponseErrorAttributes(String responseBody) {
        try {
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {
            };

            return objectMapper.readValue(responseBody, typeRef);
        } catch (IOException ex) {
            logNotSafe.info("It is was not possible deserialize API exception body to response error attributes! " +
                "Message: {}", ex.getMessage());
            return null;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean appendResponseStr(@NonNull StringBuilder remoteExceptionStr, Object responseObject) {
        if (responseObject == null) {
            return false;
        }

        try {
            String prettyPrintedResponseBody =
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseObject);
            remoteExceptionStr.append(prettyPrintedResponseBody);
            return true;
        } catch (IOException ex) {
            logNotSafe.info("It is was not possible append the response exception, neither the response error attributes! " +
                "Message: {}", ex.getMessage());
            return false;
        }
    }

    @ExceptionHandler({IllegalArgumentApplicationException.class, IllegalStateApplicationException.class})
    public ResponseEntity<ResponseExceptionDTO> handleIllegalArgumentStateApplicationException(
        HttpServletRequest request, ApplicationException ex
    ) {
        return handleApplicationException(request, HttpStatus.NOT_ACCEPTABLE, ex);
    }

    @ExceptionHandler(SecurityApplicationException.class)
    public ResponseEntity<ResponseExceptionDTO> handleSecurityApplicationException(
        HttpServletRequest request, SecurityApplicationException ex
    ) {
        return handleApplicationException(request, HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(ResourceNotFoundApplicationException.class)
    public ResponseEntity<ResponseExceptionDTO> handleResourceNotFoundApplicationException(
        HttpServletRequest request, ResourceNotFoundApplicationException ex
    ) {
        return handleApplicationException(request, HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ResponseExceptionDTO> handleApplicationException(
        HttpServletRequest request, ApplicationException ex
    ) {
        return handleApplicationException(request, DEFAULT_HTTP_STATUS, ex);
    }

    @NonNull
    private ResponseEntity<ResponseExceptionDTO> handleApplicationException(
        HttpServletRequest request, HttpStatusCode statusCode, ApplicationException ex
    ) {
        return handleException(request, statusCode, ex, ex.getArgs());
    }

    @ExceptionHandler(RestApiException.class)
    public ResponseEntity<ResponseExceptionDTO> handleRestApiException(
        HttpServletRequest request, RestApiException ex
    ) {
        HttpStatusCode statusCode = getHttpStatusCode(ex);
        return handleException(request, statusCode, ex, ex.getArgs());
    }

    /**
     * Thrown when used the @{@link org.springframework.validation.annotation.Validated} annotation.
     */
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ResponseExceptionDTO> handleJavaxConstraintViolationException(
        HttpServletRequest request, jakarta.validation.ConstraintViolationException ex
    ) {
        String errorMsg = getMessage(
            request.getLocale(), DEFAULT_VALIDATION_ERROR_CODE, DEFAULT_VALIDATION_ERROR_MESSAGE
        );

        List<String> validationErrors = new ArrayList<>();
        StringBuilder validationErrorsMsg = new StringBuilder();
        for (final ConstraintViolation<?> constraintViolation : ex.getConstraintViolations()) {
            String validationErrorMsg = constraintViolation.getPropertyPath() + ":" + constraintViolation.getMessage();
            validationErrors.add(validationErrorMsg);
            validationErrorsMsg
                .append(System.lineSeparator())
                .append("Validation: ")
                .append(validationErrorMsg);
        }

        UUID uuid = logWarnMessage(request, errorMsg + validationErrorsMsg);
        ValidationResponseExceptionDTO responseException =
            new ValidationResponseExceptionDTO(
                uuid,
                DEFAULT_VALIDATION_ERROR_CODE,
                errorMsg,
                HttpStatus.BAD_REQUEST.value(),
                validationErrors.toArray(new String[]{})
            );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseException);
    }

    /**
     * Thrown when used the @{@link jakarta.validation.Valid} annotation.
     */
    @NotNull
    @Override
    public ResponseEntity<Object> handleMethodArgumentNotValid(
        MethodArgumentNotValidException ex,
        @NotNull HttpHeaders headers,
        @NotNull HttpStatusCode statusCode,
        WebRequest request
    ) {
        String errorMsg = getMessage(
            request.getLocale(), DEFAULT_VALIDATION_ERROR_CODE, DEFAULT_VALIDATION_ERROR_MESSAGE
        );

        List<String> validationErrors = new ArrayList<>();
        StringBuilder validationErrorsMsg = new StringBuilder();
        for (final FieldError error : ex.getBindingResult().getFieldErrors()) {
            String validationErrorMsg = error.getField() + ":" + error.getDefaultMessage();
            validationErrors.add(validationErrorMsg);
            validationErrorsMsg
                .append(System.lineSeparator())
                .append("Validation: ")
                .append(validationErrorMsg);
        }

        UUID uuid = logWarnMessage(request, errorMsg + validationErrorsMsg);
        ValidationResponseExceptionDTO responseException =
            new ValidationResponseExceptionDTO(
                uuid,
                DEFAULT_VALIDATION_ERROR_CODE,
                errorMsg,
                HttpStatus.BAD_REQUEST.value(),
                validationErrors.toArray(new String[]{})
            );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(responseException);
    }

    @ExceptionHandler(TransactionException.class)
    public ResponseEntity<ResponseExceptionDTO> handleTransactionException(
        HttpServletRequest request, TransactionException ex
    ) {
        ConstraintViolationException constraintEx = getConstraintViolationException(ex.getCause());
        if (constraintEx != null) {
            return handleConstraintViolationException(request, constraintEx);
        } else {
            return handleThrowable(request, ex);
        }
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseExceptionDTO> handleDataIntegrityViolationException(
        HttpServletRequest request, DataIntegrityViolationException ex
    ) {
        ConstraintViolationException constraintEx = getConstraintViolationException(ex.getCause());
        if (constraintEx != null) {
            return handleConstraintViolationException(request, constraintEx);
        } else {
            return handleThrowable(request, ex);
        }
    }

    private ResponseEntity<ResponseExceptionDTO> handleConstraintViolationException(
        HttpServletRequest request, ConstraintViolationException ex
    ) {
        String errorCode = getConstraintViolationErrorCode(ex);
        String errorMsg = getMessage(request.getLocale(), errorCode, DEFAULT_ERROR_MESSAGE);
        UUID uuid = logMessage(request, errorMsg, ex);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
            new ResponseExceptionDTO(uuid, errorCode, errorMsg, HttpStatus.CONFLICT.value())
        );
    }

    @ExceptionHandler(EmptyResultDataAccessException.class)
    public ResponseEntity<ResponseExceptionDTO> handleEmptyResultDataAccessException(
        HttpServletRequest request, EmptyResultDataAccessException ex
    ) {
        return handleHttpErrorException(request, HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseExceptionDTO> handleResourceNotFoundException(
        HttpServletRequest request, ResourceNotFoundException ex
    ) {
        return handleHttpErrorException(request, HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(UndeclaredThrowableException.class)
    public ResponseEntity<ResponseExceptionDTO> handleResourceNotFoundException(
        HttpServletRequest request, UndeclaredThrowableException ex
    ) {
        return handleThrowable(request, ex.getUndeclaredThrowable());
    }

    @NotNull
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
        @NotNull Exception ex,
        @Nullable Object body,
        @NotNull HttpHeaders headers,
        @NotNull HttpStatusCode statusCode,
        @NotNull WebRequest request
    ) {
        if (DEFAULT_HTTP_STATUS.equals(statusCode)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex, RequestAttributes.SCOPE_REQUEST);
        }

        String errorCode = HTTP_ERROR_PREFIX + statusCode.value();
        String errorMsg = getMessage(request.getLocale(), errorCode, DEFAULT_ERROR_MESSAGE);
        UUID uuid = logErrorMessage(request, errorMsg, ex);
        return ResponseEntity.status(statusCode).body(
            new ResponseExceptionDTO(uuid, errorCode, errorMsg, statusCode.value())
        );
    }

    @NotNull
    protected ResponseEntity<ResponseExceptionDTO> handleHttpErrorException(
        HttpServletRequest request, HttpStatusCode statusCode, Throwable ex
    ) {
        if (DEFAULT_HTTP_STATUS.equals(statusCode)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
        }

        String errorCode = HTTP_ERROR_PREFIX + statusCode.value();
        String errorMsg = getMessage(request.getLocale(), errorCode, DEFAULT_ERROR_MESSAGE);
        UUID uuid = logMessage(request, errorMsg, ex);
        return ResponseEntity.status(statusCode).body(
            new ResponseExceptionDTO(uuid, errorCode, errorMsg, statusCode.value())
        );
    }

    private ResponseEntity<ResponseExceptionDTO> handleException(
        HttpServletRequest request, HttpStatusCode statusCode, Exception ex, Serializable... args
    ) {
        if (DEFAULT_HTTP_STATUS.equals(statusCode)) {
            request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, ex);
        }

        String errorCode = ex.getMessage();
        String errorMsg = getMessage(request.getLocale(), errorCode, DEFAULT_ERROR_MESSAGE, args);
        UUID uuid = logMessage(request, errorMsg, ex);
        return ResponseEntity.status(statusCode).body(
            new ResponseExceptionDTO(uuid, errorCode, errorMsg, statusCode.value(), args)
        );
    }

    private UUID logMessage(HttpServletRequest request, String errorMessage) {
        return logMessage(request, errorMessage, null);
    }

    private UUID logMessage(HttpServletRequest request, String errorMessage, Throwable ex) {
        UUID uuid = UUID.randomUUID();
        if (shouldLogAsWarning(ex, errorMessage)) {
            String msg = "Requested URL: " + request.getRequestURL()
                + System.lineSeparator() + "\tWarn UUID: " + uuid + " | " + errorMessage;
            logNotSafe.warn(msg, ex);
        } else {
            String msg = "Requested URL: " + request.getRequestURL()
                + System.lineSeparator() + "\tError UUID: " + uuid + " | " + errorMessage;
            logNotSafe.error(msg, ex);
        }
        return uuid;
    }

    private boolean shouldLogAsWarning(Throwable throwable, String errorMessage) {
        return shouldLogAsWarning.entrySet().stream()
            .anyMatch(entry -> {
                String message = "";
                boolean isSameClass = false;
                try {
                    if (throwable == null) {
                        if (entry.getKey().equals("null")) {
                            isSameClass = true;
                            message = errorMessage;
                        }
                    } else {
                        isSameClass = throwable.getClass().isAssignableFrom(Class.forName(entry.getKey()));
                        message = throwable.getMessage();
                    }
                } catch (ClassNotFoundException cnfe) {
                    // Ignore! Pois já existe uma validação no Bootstrap da aplicação.
                }

                if (isSameClass) {
                    List<String> msgsList = entry.getValue();
                    if (msgsList.isEmpty()) {
                        return true;
                    } else {
                        for (String msg : msgsList) {
                            if (StringUtils.contains(message, msg)) {
                                return true;
                            }
                        }
                    }
                }

                return false;
            });
    }

    private UUID logWarnMessage(HttpServletRequest request, String errorMessage) {
        UUID uuid = UUID.randomUUID();
        logNotSafe.warn("Requested URL: {}{}\tWarn UUID: {} | {}",
            request.getRequestURL(),
            System.lineSeparator(),
            uuid,
            errorMessage
        );
        return uuid;
    }

    private UUID logWarnMessage(WebRequest request, String errorMessage) {
        UUID uuid = UUID.randomUUID();
        logNotSafe.warn("Requested URL: {}{}\tWarn UUID: {} | {}",
            ((ServletWebRequest) request).getRequest().getRequestURI(),
            System.lineSeparator(),
            uuid,
            errorMessage
        );
        return uuid;
    }

    private UUID logErrorMessage(WebRequest request, String errorMessage, Throwable ex) {
        UUID uuid = UUID.randomUUID();
        logNotSafe.error("Requested URL: {}{}\tError UUID: {} | {}",
            ((ServletWebRequest) request).getRequest().getRequestURI(),
            System.lineSeparator(),
            uuid,
            errorMessage,
            ex
        );
        return uuid;
    }

    private HttpStatusCode getHttpStatusCode(Exception ex) {
        ResponseStatus responseStatus =
            AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            return responseStatus.code();
        } else if (ex.getCause() instanceof Exception) {
            ex = (Exception) ex.getCause();
            return getHttpStatusCode(ex);
        }
        return DEFAULT_HTTP_STATUS;
    }

    private ConstraintViolationException getConstraintViolationException(Throwable throwable) {
        if (throwable == null) {
            return null;
        } else if (throwable instanceof ConstraintViolationException) {
            return (ConstraintViolationException) throwable;
        } else {
            return getConstraintViolationException(throwable.getCause());
        }
    }

    private String getConstraintViolationErrorCode(ConstraintViolationException ex) {
        return switch (ex.getSQLState()) {
            case "23502" -> "not_null_" + ex.getConstraintName() + "_exception";
            case "23505" -> "unique_" + ex.getConstraintName() + "_exception";
            default -> HTTP_ERROR_PREFIX + HttpStatus.CONFLICT.value();
        };
    }

    private String getMessage(Locale locale, String code, String defaultMessage, Serializable... args) {
        return messageSource != null
            ? messageSource.getMessage(code, args, defaultMessage, locale)
            : defaultMessage;
    }
}
