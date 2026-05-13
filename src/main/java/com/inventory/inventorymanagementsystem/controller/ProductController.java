package com.inventory.inventorymanagementsystem.controller;

import com.inventory.inventorymanagementsystem.dto.ProductRequestDTO;
import com.inventory.inventorymanagementsystem.dto.ProductResponseDTO;
import com.inventory.inventorymanagementsystem.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponseDTO createProduct(
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        log.info("API Request: POST /api/products - Creating new product with name: {}", requestDTO.getName());

        ProductResponseDTO response = productService.saveProduct(requestDTO);
        log.info("API Response: Product created successfully with ID: {}", response.getId());

        return response;
    }

    @GetMapping
    public List<ProductResponseDTO> getAllProducts() {
        log.info("API Request: GET /api/products - Fetching all products");

        List<ProductResponseDTO> products = productService.getAllProducts();
        log.info("API Response: Retrieved {} products", products.size());

        return products;
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(@PathVariable Long id) {
        log.info("API Request: GET /api/products/{} - Fetching product by ID", id);

        ProductResponseDTO product = productService.getProductById(id);
        log.info("API Response: Product retrieved - ID: {}, Name: {}", id, product.getName());

        return product;
    }

    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {
        log.info("API Request: PUT /api/products/{} - Updating product", id);

        ProductResponseDTO response = productService.updateProduct(id, requestDTO);
        log.info("API Response: Product updated successfully - ID: {}", id);

        return response;
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {
        log.info("API Request: DELETE /api/products/{} - Deleting product", id);

        productService.deleteProduct(id);

        log.info("API Response: Product deleted successfully - ID: {}", id);
        return "Product deleted successfully";
    }
}
