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
        try {
            ShoppingCart cart = getOrCreateShoppingCart(username);
            ShoppingCartDto dto = shoppingCartMapper.toDto(cart);
            log.info("Retrieved shopping cart for user: {}, cart: {}", username, dto);
            return dto;
        } catch (Exception e) {
            log.error("Error getting shopping cart for user: {}", username, e);
            throw new RuntimeException("Failed to get shopping cart: " + e.getMessage());
        }
    }

    @Transactional
    public ShoppingCartDto addProductToShoppingCart(String username, Map<UUID, Integer> productList) {
        log.info("Adding products to cart for user: {}, products: {}", username, productList);

        try {
            validateProductsAvailability(productList);

            ShoppingCart cart = addProductsToCart(username, productList);
            ShoppingCartDto dto = shoppingCartMapper.toDto(cart);
            log.info("Successfully added products to cart for user: {}", username);
            return dto;
        } catch (ProductInShoppingCartLowQuantityInWarehouse | WarehouseServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error adding products to cart for user: {}", username, e);
            throw new RuntimeException("Failed to add products to cart: " + e.getMessage());
        }
    }

    @Transactional
    public void deactivateCurrentShoppingCart(String username) {
        try {
            log.info("Deactivating shopping cart for user: {}", username);
            deactivateShoppingCart(username);
            log.info("Successfully deactivated shopping cart for user: {}", username);
        } catch (NoProductsInShoppingCartException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error deactivating shopping cart for user: {}", username, e);
            throw new RuntimeException("Failed to deactivate shopping cart: " + e.getMessage());
        }
    }

    @Transactional
    public ShoppingCartDto removeFromShoppingCart(String username, List<UUID> productIds) {
        try {
            log.info("Removing products from cart for user: {}, productIds: {}", username, productIds);

            ShoppingCart cart = removeProductsFromCart(username, productIds);
            ShoppingCartDto dto = shoppingCartMapper.toDto(cart);
            log.info("Successfully removed products from cart for user: {}", username);
            return dto;
        } catch (NoProductsInShoppingCartException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error removing products from cart for user: {}", username, e);
            throw new RuntimeException("Failed to remove products from cart: " + e.getMessage());
        }
    }

    @Transactional
    public ShoppingCartDto changeProductQuantity(String username, ChangeProductQuantityRequest request) {
        log.info("Changing product quantity for user: {}, product: {}, quantity: {}",
                username, request.getProductId(), request.getNewQuantity());

        try {
            if (request.getNewQuantity() > 0) {
                Map<UUID, Integer> productQuantity = Map.of(request.getProductId(), request.getNewQuantity());
                validateProductsAvailability(productQuantity);
            }

            ShoppingCart cart = changeProductQuantityInternal(username, request.getProductId(), request.getNewQuantity());
            ShoppingCartDto dto = shoppingCartMapper.toDto(cart);
            log.info("Successfully changed product quantity for user: {}", username);
            return dto;
        } catch (ProductInShoppingCartLowQuantityInWarehouse | WarehouseServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error changing product quantity for user: {}", username, e);
            throw new RuntimeException("Failed to change product quantity: " + e.getMessage());
        }
    }

    private void validateProductsAvailability(Map<UUID, Integer> productList) {
        try {
            log.debug("Checking product availability in warehouse: {}", productList);
            warehouseClient.checkProductQuantityEnoughForShoppingCart(productList);
            log.info("Products availability check passed for: {}", productList.keySet());
        } catch (Exception e) {
            log.warn("Product availability check failed: {}", e.getMessage());

            if (e.getMessage() != null && e.getMessage().contains("not found") ||
                    e.getMessage().contains("Not enough quantity")) {
                throw new ProductInShoppingCartLowQuantityInWarehouse("Product not available or insufficient quantity in warehouse");
            } else if (e.getMessage() != null && e.getMessage().contains("unavailable")) {
                throw new WarehouseServiceUnavailableException("Warehouse service temporarily unavailable");
            } else {
                throw new WarehouseServiceUnavailableException("Warehouse service error: " + e.getMessage());
            }
        }
    }

    public ShoppingCart getOrCreateShoppingCart(String username) {
        return shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createNewShoppingCart(username));
    }

    @Transactional
    public ShoppingCart addProductsToCart(String username, Map<UUID, Integer> products) {
        ShoppingCart cart = getOrCreateShoppingCart(username);

        if (cart.getProducts() == null) {
            cart.setProducts(new java.util.HashMap<>());
        }

        products.forEach((productId, quantity) -> {
            if (quantity > 0) {
                cart.getProducts().merge(productId, quantity, Integer::sum);
            }
        });

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart removeProductsFromCart(String username, List<UUID> productIds) {
        ShoppingCart cart = getOrCreateShoppingCart(username);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            throw new NoProductsInShoppingCartException("No products found in cart");
        }

        boolean removed = productIds.stream()
                .anyMatch(productId -> cart.getProducts().remove(productId) != null);

        if (!removed) {
            throw new NoProductsInShoppingCartException("No specified products found in cart");
        }

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart changeProductQuantityInternal(String username, UUID productId, Integer newQuantity) {
        ShoppingCart cart = getOrCreateShoppingCart(username);

        if (cart.getProducts() == null || !cart.getProducts().containsKey(productId)) {
            throw new NoProductsInShoppingCartException("Product not found in cart");
        }

        if (newQuantity == 0) {
            cart.getProducts().remove(productId);
        } else {
            cart.getProducts().put(productId, newQuantity);
        }

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public void deactivateShoppingCart(String username) {
        ShoppingCart cart = shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new NoProductsInShoppingCartException("Active cart not found for user: " + username));

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
