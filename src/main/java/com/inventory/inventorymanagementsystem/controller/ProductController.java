package com.inventory.inventorymanagementsystem.controller;

import com.inventory.inventorymanagementsystem.dto.ProductRequestDTO;
import com.inventory.inventorymanagementsystem.dto.ProductResponseDTO;
import com.inventory.inventorymanagementsystem.entity.Product;
import com.inventory.inventorymanagementsystem.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        return productService.saveProduct(requestDTO);
    }

    @GetMapping
    public List<ProductResponseDTO> getAllProducts() {

        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ProductResponseDTO getProductById(@PathVariable Long id) {

        return productService.getProductById(id);
    }

    @PutMapping("/{id}")
    public ProductResponseDTO updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO requestDTO) {

        return productService.updateProduct(id, requestDTO);
    }

    @DeleteMapping("/{id}")
    public String deleteProduct(@PathVariable Long id) {

        productService.deleteProduct(id);

        return "Product deleted successfully";
    }
}

