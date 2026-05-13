package com.inventory.inventorymanagementsystem.service;

import com.inventory.inventorymanagementsystem.dto.ProductRequestDTO;
import com.inventory.inventorymanagementsystem.dto.ProductResponseDTO;
import com.inventory.inventorymanagementsystem.entity.Product;
import com.inventory.inventorymanagementsystem.exception.ProductNotFoundException;
import com.inventory.inventorymanagementsystem.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponseDTO saveProduct(ProductRequestDTO requestDTO) {

        Product product = new Product();

        product.setName(requestDTO.getName());
        product.setBrand(requestDTO.getBrand());
        product.setPrice(requestDTO.getPrice());
        product.setStockQuantity(requestDTO.getStockQuantity());

        Product savedProduct = productRepository.save(product);

        return mapToResponseDTO(savedProduct);
    }

    public List<ProductResponseDTO> getAllProducts() {

        return productRepository.findAll()
                .stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    public ProductResponseDTO getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        return mapToResponseDTO(product);
    }

    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO requestDTO) {

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        existingProduct.setName(requestDTO.getName());
        existingProduct.setBrand(requestDTO.getBrand());
        existingProduct.setPrice(requestDTO.getPrice());
        existingProduct.setStockQuantity(requestDTO.getStockQuantity());

        Product updatedProduct = productRepository.save(existingProduct);

        return mapToResponseDTO(updatedProduct);
    }

    public void deleteProduct(Long id) {

        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(
                                "Product not found with id: " + id
                        )
                );

        productRepository.delete(existingProduct);
    }

    private ProductResponseDTO mapToResponseDTO(Product product) {

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}

