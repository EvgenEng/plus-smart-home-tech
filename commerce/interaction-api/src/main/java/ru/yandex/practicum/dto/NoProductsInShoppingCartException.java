package ru.yandex.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoProductsInShoppingCartException extends RuntimeException {
    private String message;
    private String userMessage;
    private HttpStatus httpStatus;
}
