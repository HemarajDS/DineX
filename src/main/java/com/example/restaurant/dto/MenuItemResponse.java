package com.example.restaurant.dto;

import java.math.BigDecimal;

public class MenuItemResponse {

    private String id;
    private String name;
    private BigDecimal price;
    private String type;
    private String category;
    private String imageUrl;
    private String description;
    private boolean available;

    public MenuItemResponse() {
    }

    public MenuItemResponse(String id, String name, BigDecimal price, String type, String category, String imageUrl, String description, boolean available) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.type = type;
        this.category = category;
        this.imageUrl = imageUrl;
        this.description = description;
        this.available = available;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }
}
