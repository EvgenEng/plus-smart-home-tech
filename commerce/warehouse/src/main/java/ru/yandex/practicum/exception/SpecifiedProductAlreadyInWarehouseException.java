package ru.yandex.practicum.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpecifiedProductAlreadyInWarehouseException extends RuntimeException {
    private String message;
    private String userMessage;
    private HttpStatus httpStatus;

    public SpecifiedProductAlreadyInWarehouseException(String message) {
        this.message = message;
        this.userMessage = "Product already exists in warehouse";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
}
