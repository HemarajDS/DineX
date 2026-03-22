package com.example.restaurant.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class BillSummaryRequest {

    @NotBlank(message = "Table number is required")
    private String tableNumber;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount amount cannot be negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0.0", inclusive = true, message = "Discount percent cannot be negative")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0.0", inclusive = true, message = "GST percent cannot be negative")
    private BigDecimal gstPercent;

    @DecimalMin(value = "0.0", inclusive = true, message = "Additional tax percent cannot be negative")
    private BigDecimal additionalTaxPercent;

    @DecimalMin(value = "0.0", inclusive = true, message = "Service charge percent cannot be negative")
    private BigDecimal serviceChargePercent;

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent;
    }

    public BigDecimal getGstPercent() {
        return gstPercent;
    }

    public void setGstPercent(BigDecimal gstPercent) {
        this.gstPercent = gstPercent;
    }

    public BigDecimal getAdditionalTaxPercent() {
        return additionalTaxPercent;
    }

    public void setAdditionalTaxPercent(BigDecimal additionalTaxPercent) {
        this.additionalTaxPercent = additionalTaxPercent;
    }

    public BigDecimal getServiceChargePercent() {
        return serviceChargePercent;
    }

    public void setServiceChargePercent(BigDecimal serviceChargePercent) {
        this.serviceChargePercent = serviceChargePercent;
    }
}
