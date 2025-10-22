package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.exception.NoProductsInShoppingCartException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.model.ShoppingCart;
import ru.yandex.practicum.repository.ShoppingCartRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShoppingCartService {
    private final ShoppingCartRepository shoppingCartRepository;

    public ShoppingCart getOrCreateShoppingCart(String username) {
        validateUsername(username);
        return shoppingCartRepository.findByUsernameAndActiveTrue(username)
                .orElseGet(() -> createNewShoppingCart(username));
    }

    @Transactional
    public ShoppingCart addProductsToCart(String username, Map<UUID, Integer> products) {
        validateUsername(username);
        ShoppingCart cart = getOrCreateShoppingCart(username);

        products.forEach((productId, quantity) -> {
            cart.getProducts().merge(productId, quantity, Integer::sum);
        });

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart removeProductsFromCart(String username, List<UUID> productIds) {
        validateUsername(username);
        ShoppingCart cart = getOrCreateShoppingCart(username);

        boolean removed = productIds.stream()
                .anyMatch(productId -> cart.getProducts().remove(productId) != null);

        if (!removed) {
            throw new NoProductsInShoppingCartException("No products found in cart");
        }

        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public ShoppingCart changeProductQuantity(String username, UUID productId, Integer newQuantity) {
        validateUsername(username);
        ShoppingCart cart = getOrCreateShoppingCart(username);

        if (!cart.getProducts().containsKey(productId)) {
            throw new NoProductsInShoppingCartException("Product not found in cart");
        }

        cart.getProducts().put(productId, newQuantity);
        return shoppingCartRepository.save(cart);
    }

    @Transactional
    public void deactivateShoppingCart(String username) {
        validateUsername(username);
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

    private void validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new NotAuthorizedUserException("Username is required");
        }
    }
}
