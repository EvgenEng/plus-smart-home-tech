package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouse.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleLowQuantity(ProductInShoppingCartLowQuantityInWarehouse ex) {
        log.warn("Low quantity error: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(WarehouseServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleWarehouseUnavailable(WarehouseServiceUnavailableException ex) {
        log.warn("Warehouse unavailable: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(NoProductsInShoppingCartException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoProducts(NoProductsInShoppingCartException ex) {
        log.warn("No products in cart: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return new ErrorResponse(ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception: {}", ex.getMessage(), ex);
        return new ErrorResponse("Internal server error: " + ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGenericException(Exception ex) {
        log.error("Internal server error: {}", ex.getMessage(), ex);
        return new ErrorResponse("Internal server error");
    }
}
