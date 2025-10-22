package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.model.WarehouseProduct;
import ru.yandex.practicum.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseService {
    private final WarehouseProductRepository warehouseProductRepository;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS = ADDRESSES[new SecureRandom().nextInt(ADDRESSES.length)];

    @Transactional
    public void addNewProduct(NewProductInWarehouseRequest request) {
        if (warehouseProductRepository.existsByProductId(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException("Product already in warehouse");
        }

        WarehouseProduct product = WarehouseProduct.builder()
                .productId(request.getProductId())
                .quantity(0)
                .fragile(request.getFragile() != null ? request.getFragile() : false)
                .width(request.getDimension().getWidth())
                .height(request.getDimension().getHeight())
                .depth(request.getDimension().getDepth())
                .weight(request.getWeight())
                .build();

        warehouseProductRepository.save(product);
    }

    @Transactional
    public void addProductQuantity(AddProductToWarehouseRequest request) {
        WarehouseProduct product = warehouseProductRepository.findByProductId(request.getProductId())
                .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse"));

        product.setQuantity(product.getQuantity() + request.getQuantity());
        warehouseProductRepository.save(product);
    }

    public BookedProductsDto checkProductQuantity(ShoppingCartDto shoppingCart) {
        Double totalWeight = 0.0;
        Double totalVolume = 0.0;
        Boolean hasFragile = false;

        for (Map.Entry<UUID, Integer> entry : shoppingCart.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer requestedQuantity = entry.getValue();

            WarehouseProduct warehouseProduct = warehouseProductRepository.findByProductId(productId)
                    .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse"));

            if (warehouseProduct.getQuantity() < requestedQuantity) {
                throw new ProductInShoppingCartLowQuantityInWarehouse("Not enough quantity in warehouse");
            }

            Double productWeight = warehouseProduct.getWeight() != null ? warehouseProduct.getWeight() : 1.0;
            Double productVolume = warehouseProduct.getWidth() * warehouseProduct.getHeight() * warehouseProduct.getDepth();

            totalWeight += productWeight * requestedQuantity;
            totalVolume += productVolume * requestedQuantity;

            if (warehouseProduct.getFragile() != null && warehouseProduct.getFragile()) {
                hasFragile = true;
            }
        }

        return new BookedProductsDto(totalWeight, totalVolume, hasFragile);
    }

    public AddressDto getWarehouseAddress() {
        return new AddressDto("Россия", "Москва", "Ленинградский проспект", "80", "1");
    }
}
