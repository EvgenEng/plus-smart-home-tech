package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.AddProductToWarehouseRequest;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.mapper.WarehouseProductMapper;
import ru.yandex.practicum.model.WarehouseProduct;
import ru.yandex.practicum.repository.WarehouseProductRepository;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class WarehouseService {
    private final WarehouseProductRepository warehouseProductRepository;
    private final WarehouseProductMapper warehouseProductMapper;

    private static final String[] ADDRESSES = new String[]{"ADDRESS_1", "ADDRESS_2"};
    private static final String CURRENT_ADDRESS = ADDRESSES[new SecureRandom().nextInt(ADDRESSES.length)];

    @Transactional
    public void addNewProduct(NewProductInWarehouseRequest request) {
        if (warehouseProductRepository.existsByProductId(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException("Product already in warehouse");
        }

        WarehouseProduct product = warehouseProductMapper.toWarehouseProduct(request);
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
        log.info("Checking product quantity for shopping cart: {}", shoppingCart);

        // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: проверяем на null
        if (shoppingCart == null || shoppingCart.getProducts() == null || shoppingCart.getProducts().isEmpty()) {
            log.info("Empty shopping cart received");
            return new BookedProductsDto(0.0, 0.0, false);
        }

        AtomicBoolean hasFragile = new AtomicBoolean(false);

        Map<UUID, Integer> productsSummary = shoppingCart.getProducts().entrySet().stream()
                .peek(entry -> {
                    UUID productId = entry.getKey();
                    Integer requestedQuantity = entry.getValue();

                    WarehouseProduct warehouseProduct = warehouseProductRepository.findByProductId(productId)
                            .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse: " + productId));

                    if (warehouseProduct.getQuantity() < requestedQuantity) {
                        throw new ProductInShoppingCartLowQuantityInWarehouse(
                                "Not enough quantity in warehouse for product: " + productId +
                                        ". Available: " + warehouseProduct.getQuantity() + ", requested: " + requestedQuantity
                        );
                    }

                    if (warehouseProduct.getFragile() != null && warehouseProduct.getFragile()) {
                        hasFragile.set(true);
                    }
                })
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        double totalWeight = productsSummary.entrySet().stream()
                .mapToDouble(entry -> {
                    WarehouseProduct product = warehouseProductRepository.findByProductId(entry.getKey())
                            .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse"));
                    Double productWeight = product.getWeight() != null ? product.getWeight() : 1.0;
                    return productWeight * entry.getValue();
                })
                .sum();

        double totalVolume = productsSummary.entrySet().stream()
                .mapToDouble(entry -> {
                    WarehouseProduct product = warehouseProductRepository.findByProductId(entry.getKey())
                            .orElseThrow(() -> new NoSpecifiedProductInWarehouseException("Product not found in warehouse"));
                    Double productVolume = product.getWidth() * product.getHeight() * product.getDepth();
                    return productVolume * entry.getValue();
                })
                .sum();

        return new BookedProductsDto(totalWeight, totalVolume, hasFragile.get());
    }

    public AddressDto getWarehouseAddress() {
        return new AddressDto(CURRENT_ADDRESS, CURRENT_ADDRESS, CURRENT_ADDRESS, CURRENT_ADDRESS, CURRENT_ADDRESS);
    }
}
