package ru.yandex.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.ShoppingCartDto;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final WarehouseClient warehouseClient;

    @PostMapping("/debug-warehouse")
    public String debugWarehouse(@RequestBody Map<UUID, Integer> productList) {
        log.info("Testing warehouse call with: {}", productList);

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(productList);
            return "SUCCESS - Warehouse call worked!";
        } catch (Exception e) {
            log.error("Warehouse call failed: {}", e.getMessage(), e);
            return "ERROR - " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }
}
