package com.shop.demo.service;

import com.shop.demo.entity.Product;
import jakarta.validation.Valid;

import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    Product getProductById(Long id);

    int addProduct(Product product);
}