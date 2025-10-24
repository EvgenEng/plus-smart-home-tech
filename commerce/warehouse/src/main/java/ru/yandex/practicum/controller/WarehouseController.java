package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.service.WarehouseService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController implements WarehouseClient {
    private final WarehouseService warehouseService;

    @Override
    @GetMapping("/address")
    public AddressDto getWarehouseAddress() {
        return warehouseService.getWarehouseAddress();
    }

    @Override
    @PutMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addNewProduct(@Valid @RequestBody NewProductInWarehouseRequest request) {
        warehouseService.addNewProduct(request);
    }

    @Override
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.OK)
    public void addProductQuantity(@Valid @RequestBody AddProductToWarehouseRequest request) {
        warehouseService.addProductQuantity(request);
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkProductQuantity(@Valid @RequestBody ShoppingCartDto shoppingCart) {
        return warehouseService.checkProductQuantity(shoppingCart);
    }

    @PostMapping("/check-quantity")
    @ResponseStatus(HttpStatus.OK)
    public void checkProductQuantityEnoughForShoppingCart(@RequestBody Map<UUID, Long> productList) {
        ShoppingCartDto shoppingCartDto = new ShoppingCartDto();
        Map<UUID, Integer> products = new HashMap<>();
        productList.forEach((key, value) -> products.put(key, value.intValue()));
        shoppingCartDto.setProducts(products);

        warehouseService.checkProductQuantity(shoppingCartDto);
    }
}
