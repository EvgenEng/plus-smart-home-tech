package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductState;
import ru.yandex.practicum.dto.QuantityState;
import ru.yandex.practicum.exception.ProductNotFoundException;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.repository.ProductRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;

    public Page<Product> getProductsByCategory(ProductCategory category, Pageable pageable) {
        return productRepository.findByProductCategoryAndProductState(category, ProductState.ACTIVE, pageable);
    }

    @Transactional
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Product product) {
        if (!productRepository.existsById(product.getProductId())) {
            throw new ProductNotFoundException("Product not found");
        }
        return productRepository.save(product);
    }

    @Transactional
    public Boolean deactivateProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        product.setProductState(ProductState.DEACTIVATE);
        productRepository.save(product);
        return true;
    }

    @Transactional
    public Boolean updateQuantityState(UUID productId, QuantityState quantityState) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        product.setQuantityState(quantityState);
        productRepository.save(product);
        return true;
    }

    public Product getProduct(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }

    public Product getActiveProduct(UUID productId) {
        return productRepository.findByProductIdAndProductState(productId, ProductState.ACTIVE)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
    }
}
