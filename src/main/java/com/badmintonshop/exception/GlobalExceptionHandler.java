package com.badmintonshop.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global Exception Handler
 * Handles exceptions for both web pages and API endpoints
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Check if request is API call
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        return uri.startsWith("/api/") || 
               (accept != null && accept.contains("application/json"));
    }

    /**
     * Handle Resource Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public Object handleEntityNotFound(EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
        }
        
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handle Resource Not Found Exception (Custom)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Object handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
        }
        
        ModelAndView mav = new ModelAndView("error/404");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handle Bad Request Exception
     */
    @ExceptionHandler(BadRequestException.class)
    public Object handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        log.warn("Bad request: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
        }
        
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handle Validation Errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        log.warn("Validation errors: {}", errors);
        
        if (isApiRequest(request)) {
            Map<String, Object> response = createErrorResponse(
                    HttpStatus.BAD_REQUEST, "Validation FAILED", request.getRequestURI());
            response.put("errors", errors);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        ModelAndView mav = new ModelAndView("error/400");
        mav.addObject("message", "Dữ liệu không hợp lệ");
        mav.addObject("errors", errors);
        return mav;
    }

    /**
     * Handle Access Denied
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse(HttpStatus.FORBIDDEN, "Bạn không có quyền truy cập", request.getRequestURI()));
        }
        
        return new ModelAndView("error/403");
    }

    /**
     * Handle Optimistic Locking Exception (Concurrent modification)
     */
    @ExceptionHandler(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
    public Object handleOptimisticLocking(Exception ex, HttpServletRequest request) {
        log.warn("Optimistic locking failure: {}", ex.getMessage());
        
        String message = "Dữ liệu đã bị thay đổi bởi người khác. Vui lòng tải lại trang và thử lại.";
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse(HttpStatus.CONFLICT, message, request.getRequestURI()));
        }
        
        ModelAndView mav = new ModelAndView("error/409");
        mav.addObject("message", message);
        return mav;
    }

    /**
     * Handle Business Logic Exception
     */
    @ExceptionHandler(BusinessException.class)
    public Object handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(createErrorResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request.getRequestURI()));
        }
        
        ModelAndView mav = new ModelAndView("error/422");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handle Insufficient Stock Exception
     */
    @ExceptionHandler(InsufficientStockException.class)
    public Object handleInsufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        log.warn("Insufficient stock: {}", ex.getMessage());
        
        if (isApiRequest(request)) {
            Map<String, Object> response = createErrorResponse(
                    HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
            response.put("productId", ex.getProductId());
            response.put("requestedQuantity", ex.getRequestedQuantity());
            response.put("availableQuantity", ex.getAvailableQuantity());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        
        ModelAndView mav = new ModelAndView("error/stock-error");
        mav.addObject("message", ex.getMessage());
        return mav;
    }

    /**
     * Handle 404 - Page Not Found
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Object handleNotFound(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("Page not found: {}", request.getRequestURI());
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse(HttpStatus.NOT_FOUND, "Không tìm thấy trang", request.getRequestURI()));
        }
        
        return new ModelAndView("error/404");
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    public Object handleAllExceptions(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: ", ex);
        
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                            "Đã xảy ra lỗi. Vui lòng thử lại sau.", request.getRequestURI()));
        }
        
        ModelAndView mav = new ModelAndView("error/500");
        mav.addObject("message", "Đã xảy ra lỗi. Vui lòng thử lại sau.");
        return mav;
    }

    /**
     * Create standard error response for API
     */
    private Map<String, Object> createErrorResponse(HttpStatus status, String message, String path) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("path", path);
        return response;
    }
}
