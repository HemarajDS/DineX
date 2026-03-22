package com.example.restaurant.dto;

public class TableResponse {

    private String id;
    private String tableNumber;
    private int capacity;
    private boolean occupied;

    public TableResponse() {
    }

    public TableResponse(String id, String tableNumber, int capacity, boolean occupied) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.capacity = capacity;
        this.occupied = occupied;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
}
