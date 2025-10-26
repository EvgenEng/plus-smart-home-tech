package ru.yandex.practicum.exception;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleFeignNotFoundException(FeignException.NotFound ex) {
        log.warn("Resource not found via Feign: {}", ex.getMessage());
        return new ErrorResponse("Requested resource not found");
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleFeignException(FeignException ex) {
        log.error("Feign client error: {}", ex.getMessage());
        return new ErrorResponse("Service temporarily unavailable");
    }

    @ExceptionHandler(WarehouseServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleWarehouseUnavailable(WarehouseServiceUnavailableException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(NoProductsInShoppingCartException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoProducts(NoProductsInShoppingCartException ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouse.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleLowQuantity(ProductInShoppingCartLowQuantityInWarehouse ex) {
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return new ErrorResponse("Internal server error");
    }
}
