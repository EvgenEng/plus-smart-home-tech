package ru.yandex.practicum.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotEnoughInfoInOrderToCalculateException extends RuntimeException {
    private String message;
    private String userMessage;
    private HttpStatus httpStatus;
}
