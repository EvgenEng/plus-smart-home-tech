package ru.yandex.practicum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.dto.ProductCategory;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.dto.ProductNotFoundException;
import ru.yandex.practicum.dto.QuantityState;
import ru.yandex.practicum.mapper.ProductMapper;
import ru.yandex.practicum.model.Product;
import ru.yandex.practicum.service.ProductService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/shopping-store")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping
    public Page<ProductDto> getProducts(
            @RequestParam ProductCategory category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "productName,asc") String sort) {

        String[] sortParams = sort.split(",");
        String sortField = sortParams[0];
        Sort.Direction direction = sortParams.length > 1 ?
                Sort.Direction.fromString(sortParams[1]) : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ProductDto> result = productService.getProductsByCategory(category, pageable)
                .map(productMapper::toDto);
        
        System.out.println("=== FORCING PAGE FIELDS ===");
        System.out.println("Page number: " + result.getNumber());
        System.out.println("Page size: " + result.getSize());
        System.out.println("Total elements: " + result.getTotalElements());
        System.out.println("Total pages: " + result.getTotalPages());
        System.out.println("First: " + result.isFirst());
        System.out.println("Last: " + result.isLast());
        System.out.println("Has next: " + result.hasNext());
        System.out.println("Has previous: " + result.hasPrevious());
        System.out.println("Sort: " + result.getSort());
        System.out.println("=== END FORCING ===");

        return result;
    }

    @PutMapping
    public ProductDto createNewProduct(@Valid @RequestBody ProductDto productDto) {
        Product product = productMapper.toEntity(productDto);
        Product savedProduct = productService.createProduct(product);
        return productMapper.toDto(savedProduct);
    }

    @PostMapping
    public ProductDto updateProduct(@Valid @RequestBody ProductDto productDto) {
        Product product = productMapper.toEntity(productDto);
        Product updatedProduct = productService.updateProduct(product);
        return productMapper.toDto(updatedProduct);
    }

    @PostMapping("/removeProductFromStore")
    @ResponseStatus(HttpStatus.OK)
    public Boolean removeProductFromStore(@RequestBody UUID productId) {
        return productService.deactivateProduct(productId);
    }

    @PostMapping("/quantityState")
    @ResponseStatus(HttpStatus.OK)
    public Boolean setProductQuantityState(
            @RequestParam UUID productId,
            @RequestParam QuantityState quantityState) {
        return productService.updateQuantityState(productId, quantityState);
    }

    @GetMapping("/{productId}")
    public ProductDto getProduct(@PathVariable UUID productId) {
        Product product = productService.getProduct(productId);
        return productMapper.toDto(product);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProductNotFoundException handleProductNotFound(ProductNotFoundException ex) {
        return ex;
    }
}
