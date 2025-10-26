package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.ChangeProductQuantityRequest;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exception.WarehouseServiceUnavailableException;
import ru.yandex.practicum.mapper.ShoppingCartMapper;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.repository.ShoppingCartRepository;
import feign.FeignException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ShoppingCartService {
    private final ShoppingCartRepository shoppingCartRepository;
    private final ShoppingCartMapper shoppingCartMapper;
    private final WarehouseClient warehouseClient;

    public ShoppingCartDto getShoppingCart(String username) {
        ShoppingCart cart = getOrCreateShoppingCart(username);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> productList) {
        log.info("Adding products to cart for user: {}, products: {}", username, productList);

        try {
            warehouseClient.checkProductQuantityEnoughForShoppingCart(productList);
        } catch (FeignException.NotFound e) {
            log.warn("Product not found in warehouse during cart addition");
            throw new ProductInShoppingCartLowQuantityInWarehouse("Product not available in warehouse");
        } catch (FeignException e) {
            log.error("Warehouse service error during cart addition: {}", e.getMessage());
            throw new WarehouseServiceUnavailableException("Warehouse service temporarily unavailable");
        }

        ShoppingCart cart = addProductsToCart(username, productList);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        deactivateShoppingCart(username);
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        ShoppingCart cart = removeProductsFromCart(username, productIds);
        return shoppingCartMapper.toDto(cart);
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        log.info("Changing product quantity for user: {}, product: {}, quantity: {}",
                username, request.getProductId(), request.getNewQuantity());

        try {
            Map<UUID, Integer> productQuantity = Map.of(request.getProductId(), request.getNewQuantity().intValue());
            warehouseClient.checkProductQuantityEnoughForShoppingCart(productQuantity);
        } catch (FeignException.NotFound e) {
            log.warn("Product not found in warehouse during quantity change");
            throw new ProductInShoppingCartLowQuantityInWarehouse("Product not available in warehouse");
        } catch (FeignException e) {
            log.error("Warehouse service error during quantity change: {}", e.getMessage());
            throw new WarehouseServiceUnavailableException("Warehouse service temporarily unavailable");
        }

        ShoppingCart cart = changeProductQuantityInternal(username, request.getProductId(), request.getNewQuantity().intValue());
        return shoppingCartMapper.toDto(cart);
    }

    public ShoppingCart getOrCreateShoppingCart(String username) {
        return shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createNewShoppingCart(username));
    }

    @Transactional
    public ShoppingCart addProductsToCart(String username, Map<UUID, Integer> products) {
        ShoppingCart cart = getOrCreateShoppingCart(username);

        products.forEach((productId, quantity) -> {
            cart.getProducts().merge(productId, quantity, Integer::sum);
        });

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart removeProductsFromCart(String username, List<UUID> productIds) {
        ShoppingCart cart = getOrCreateShoppingCart(username);

        boolean removed = productIds.stream()
                .anyMatch(productId -> cart.getProducts().remove(productId) != null);

        if (!removed) {
            throw new NoProductsInShoppingCartException("No products found in cart");
        }

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart changeProductQuantityInternal(String username, UUID productId, Integer newQuantity) {
        ShoppingCart cart = getOrCreateShoppingCart(username);

        if (!cart.getProducts().containsKey(productId)) {
            throw new NoProductsInShoppingCartException("Product not found in cart");
        }

        cart.getProducts().put(productId, newQuantity);
        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public void deactivateShoppingCart(String username) {
        ShoppingCart cart = shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NoProductsInShoppingCartException("Cart not found"));

        cart.setActive(false);
        shoppingCartRepository.save(cart);
    }

    private ShoppingCart createNewShoppingCart(String username) {
        ShoppingCart newCart = ShoppingCart.builder()
                .username(username)
                .active(true)
                .products(new java.util.HashMap<>())
                .build();
        return shoppingCartRepository.save(newCart);
    }
}
