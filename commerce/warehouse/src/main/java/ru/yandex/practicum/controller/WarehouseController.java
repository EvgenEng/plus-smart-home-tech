package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
@Slf4j
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
        log.info("Adding new product to warehouse: {}", request.getProductId());
        warehouseService.addNewProduct(request);
    }

    @Override
    @PostMapping("/add")
    @ResponseStatus(HttpStatus.OK)
    public void addProductQuantity(@Valid @RequestBody AddProductToWarehouseRequest request) {
        log.info("Adding quantity to product: {}, quantity: {}", request.getProductId(), request.getQuantity());
        warehouseService.addProductQuantity(request);
    }

    @Override
    @PostMapping("/check")
    public BookedProductsDto checkProductQuantity(@Valid @RequestBody ShoppingCartDto shoppingCart) {
        log.info("Checking product quantity for shopping cart");
        return warehouseService.checkProductQuantity(shoppingCart);
    }

    @Override
    @PostMapping("/check-quantity")
    @ResponseStatus(HttpStatus.OK)
    public void checkProductQuantityEnoughForShoppingCart(@RequestBody Map<UUID, Integer> productList) {
        log.info("Checking product quantity for product list: {}", productList);

        if (productList == null || productList.isEmpty()) {
            log.warn("Empty product list received");
            return;
        }

        ShoppingCartDto shoppingCartDto = new ShoppingCartDto();
        shoppingCartDto.setProducts(productList);

        warehouseService.checkProductQuantity(shoppingCartDto);
    }
}
