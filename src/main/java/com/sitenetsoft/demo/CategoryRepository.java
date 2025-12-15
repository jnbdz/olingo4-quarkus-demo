package com.sitenetsoft.demo;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CategoryRepository {

    private final List<Category> categories = List.of(
            new Category(1, "Hardware"),
            new Category(2, "Software"),
            new Category(3, "Office"),
            new Category(4, "Food"),
            new Category(5, "Misc")
    );

    public List<Category> findAll() {
        return categories;
    }

    public Optional<Category> findById(int id) {
        return categories.stream().filter(c -> c.getID() == id).findFirst();
    }
}
