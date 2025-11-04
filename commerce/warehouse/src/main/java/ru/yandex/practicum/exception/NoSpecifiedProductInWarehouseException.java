package ru.yandex.practicum.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoSpecifiedProductInWarehouseException extends RuntimeException {
    private String message;
    private String userMessage;
    private HttpStatus httpStatus;

    public NoSpecifiedProductInWarehouseException(String message) {
        this.message = message;
        this.userMessage = "Product not found in warehouse";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
}
