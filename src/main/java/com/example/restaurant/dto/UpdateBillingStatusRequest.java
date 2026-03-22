package com.example.restaurant.dto;

public class UpdateBillingStatusRequest {

    private boolean billed;

    public boolean isBilled() {
        return billed;
    }

    public void setBilled(boolean billed) {
        this.billed = billed;
    }
}
