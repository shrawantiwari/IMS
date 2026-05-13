package com.inventory.inventorymanagementsystem.service;

import com.inventory.inventorymanagementsystem.dto.ProductRequestDTO;
import com.inventory.inventorymanagementsystem.dto.ProductResponseDTO;
import com.inventory.inventorymanagementsystem.entity.Product;
import com.inventory.inventorymanagementsystem.exception.ProductNotFoundException;
import com.inventory.inventorymanagementsystem.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public ProductResponseDTO saveProduct(ProductRequestDTO requestDTO) {
        log.info("Saving new product - Name: {}, Brand: {}, Price: {}, Stock: {}",
                 requestDTO.getName(), requestDTO.getBrand(), requestDTO.getPrice(), requestDTO.getStockQuantity());
        try {
            Product product = new Product();
            product.setName(requestDTO.getName());
            product.setBrand(requestDTO.getBrand());
            product.setPrice(requestDTO.getPrice());
            product.setStockQuantity(requestDTO.getStockQuantity());

            Product savedProduct = productRepository.save(product);
            log.info("Product saved successfully with ID: {}", savedProduct.getId());

            return mapToResponseDTO(savedProduct);
        } catch (Exception e) {
            log.error("Error occurred while saving product", e);
            throw e;
        }
    }

    public List<ProductResponseDTO> getAllProducts() {
        log.debug("Fetching all products from database");
        try {
            List<ProductResponseDTO> products = productRepository.findAll()
                    .stream()
                    .map(this::mapToResponseDTO)
                    .toList();
            log.info("Retrieved {} products from database", products.size());
            return products;
        } catch (Exception e) {
            log.error("Error occurred while fetching all products", e);
            throw e;
        }
    }

    public ProductResponseDTO getProductById(Long id) {
        log.debug("Fetching product with ID: {}", id);
        try {
            Product product = productRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Product not found with ID: {}", id);
                        return new ProductNotFoundException("Product not found with id: " + id);
                    });
            log.info("Product retrieved successfully - ID: {}, Name: {}", id, product.getName());

            return mapToResponseDTO(product);
        } catch (ProductNotFoundException e) {
            log.error("Product not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while fetching product with ID: {}", id, e);
            throw e;
        }
    }

    public ProductResponseDTO updateProduct(
            Long id,
            ProductRequestDTO requestDTO) {
        log.info("Updating product with ID: {} - New Data: Name: {}, Brand: {}, Price: {}",
                 id, requestDTO.getName(), requestDTO.getBrand(), requestDTO.getPrice());
        try {
            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Product not found for update - ID: {}", id);
                        return new ProductNotFoundException("Product not found with id: " + id);
                    });

            existingProduct.setName(requestDTO.getName());
            existingProduct.setBrand(requestDTO.getBrand());
            existingProduct.setPrice(requestDTO.getPrice());
            existingProduct.setStockQuantity(requestDTO.getStockQuantity());

            Product updatedProduct = productRepository.save(existingProduct);
            log.info("Product updated successfully - ID: {}", updatedProduct.getId());

            return mapToResponseDTO(updatedProduct);
        } catch (ProductNotFoundException e) {
            log.error("Product not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while updating product with ID: {}", id, e);
            throw e;
        }
    }

    public void deleteProduct(Long id) {
        log.info("Deleting product with ID: {}", id);
        try {
            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> {
                        log.warn("Product not found for deletion - ID: {}", id);
                        return new ProductNotFoundException("Product not found with id: " + id);
                    });

            productRepository.delete(existingProduct);
            log.info("Product deleted successfully - ID: {}", id);
        } catch (ProductNotFoundException e) {
            log.error("Product not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while deleting product with ID: {}", id, e);
            throw e;
        }
    }

    private ProductResponseDTO mapToResponseDTO(Product product) {
        log.debug("Mapping product entity to response DTO - ID: {}", product.getId());

        return new ProductResponseDTO(
                product.getId(),
                product.getName(),
                product.getBrand(),
                product.getPrice(),
                product.getStockQuantity()
        );
    }
}
