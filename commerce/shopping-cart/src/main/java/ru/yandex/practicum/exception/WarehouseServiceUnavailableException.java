package ru.yandex.practicum.exception;

public class WarehouseServiceUnavailableException extends RuntimeException {
    public WarehouseServiceUnavailableException(String message) {
        super(message);
    }
}
