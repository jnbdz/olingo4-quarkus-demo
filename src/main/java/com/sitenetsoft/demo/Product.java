package com.sitenetsoft.demo;

public class Product {

    private Integer ID;
    private String Name;
    private Double Price;

    public Product() {
    }

    public Product(Integer ID, String name, Double price) {
        this.ID = ID;
        this.Name = name;
        this.Price = price;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public Double getPrice() {
        return Price;
    }

    public void setPrice(Double price) {
        this.Price = price;
    }
}
