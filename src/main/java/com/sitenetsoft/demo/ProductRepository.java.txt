package com.sitenetsoft.demo;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProductRepository {

    private final List<Product> products = List.of(
            new Product(1, "Foo", 10.0),
            new Product(2, "Bar", 20.5),
            new Product(3, "Baz", 42.0)
    );

    public List<Product> findAll() {
        return products;
    }

    public Optional<Product> findById(int id) {
        return products.stream()
                .filter(p -> p.getID() == id)
                .findFirst();
    }
}
