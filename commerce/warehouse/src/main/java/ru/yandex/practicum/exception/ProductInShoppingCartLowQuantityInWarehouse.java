package ru.yandex.practicum.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductInShoppingCartLowQuantityInWarehouse extends RuntimeException {
    private String message;
    private String userMessage;
    private HttpStatus httpStatus;

    public ProductInShoppingCartLowQuantityInWarehouse(String message) {
        this.message = message;
        this.userMessage = "Insufficient quantity";
        this.httpStatus = HttpStatus.BAD_REQUEST;
    }
}
