package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.model.ShoppingCart;

import java.util.HashMap;

@Component
public class ShoppingCartMapper {
    public ShoppingCartDto toDto(ShoppingCart shoppingCart) {
        if (shoppingCart == null) {
            return ShoppingCartDto.builder()
                    .shoppingCartId(null)
                    .products(new HashMap<>())
                    .build();
        }

        return ShoppingCartDto.builder()
                .shoppingCartId(shoppingCart.getShoppingCartId())
                .products(shoppingCart.getProducts() != null ?
                        new HashMap<>(shoppingCart.getProducts()) :
                        new HashMap<>())
                .build();
    }
}
