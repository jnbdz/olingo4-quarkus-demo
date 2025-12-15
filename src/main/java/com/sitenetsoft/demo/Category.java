package com.sitenetsoft.demo;

public class Category {
    private Integer ID;
    private String Name;

    public Category() {}
    public Category(Integer id, String name) { this.ID = id; this.Name = name; }

    public Integer getID() { return ID; }
    public void setID(Integer ID) { this.ID = ID; }

    public String getName() { return Name; }
    public void setName(String name) { this.Name = name; }
}