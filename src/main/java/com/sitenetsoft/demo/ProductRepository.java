package com.sitenetsoft.demo;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository {

    private final List<Product> products;

    public ProductRepository() {
        List<Product> tmp = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            Product p = new Product(i, "Product " + i, (double) (i * 1.25));
            // 5 categories: 1..5
            p.setCategoryID(((i - 1) % 5) + 1);
            tmp.add(p);
        }
        this.products = List.copyOf(tmp);
    }

    public List<Product> findAll() {
        return products;
    }

    public Optional<Product> findById(int id) {
        return products.stream().filter(p -> p.getID() == id).findFirst();
    }

    public List<Product> findByCategoryId(int categoryId) {
        return products.stream().filter(p -> p.getCategoryID() == categoryId).toList();
    }
}
