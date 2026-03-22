package com.example.restaurant.dto;

import java.math.BigDecimal;
import java.util.List;

public class BillSummaryResponse {

    private String receiptNumber;
    private String tableNumber;
    private int orderCount;
    private int guestCount;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxableAmount;
    private BigDecimal gstPercent;
    private BigDecimal gstAmount;
    private BigDecimal additionalTaxPercent;
    private BigDecimal additionalTaxAmount;
    private BigDecimal serviceChargePercent;
    private BigDecimal serviceChargeAmount;
    private BigDecimal grandTotal;
    private List<ReceiptLineResponse> lines;

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public String getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(String tableNumber) {
        this.tableNumber = tableNumber;
    }

    public int getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(int orderCount) {
        this.orderCount = orderCount;
    }

    public int getGuestCount() {
        return guestCount;
    }

    public void setGuestCount(int guestCount) {
        this.guestCount = guestCount;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTaxableAmount() {
        return taxableAmount;
    }

    public void setTaxableAmount(BigDecimal taxableAmount) {
        this.taxableAmount = taxableAmount;
    }

    public BigDecimal getGstPercent() {
        return gstPercent;
    }

    public void setGstPercent(BigDecimal gstPercent) {
        this.gstPercent = gstPercent;
    }

    public BigDecimal getGstAmount() {
        return gstAmount;
    }

    public void setGstAmount(BigDecimal gstAmount) {
        this.gstAmount = gstAmount;
    }

    public BigDecimal getAdditionalTaxPercent() {
        return additionalTaxPercent;
    }

    public void setAdditionalTaxPercent(BigDecimal additionalTaxPercent) {
        this.additionalTaxPercent = additionalTaxPercent;
    }

    public BigDecimal getAdditionalTaxAmount() {
        return additionalTaxAmount;
    }

    public void setAdditionalTaxAmount(BigDecimal additionalTaxAmount) {
        this.additionalTaxAmount = additionalTaxAmount;
    }

    public BigDecimal getServiceChargePercent() {
        return serviceChargePercent;
    }

    public void setServiceChargePercent(BigDecimal serviceChargePercent) {
        this.serviceChargePercent = serviceChargePercent;
    }

    public BigDecimal getServiceChargeAmount() {
        return serviceChargeAmount;
    }

    public void setServiceChargeAmount(BigDecimal serviceChargeAmount) {
        this.serviceChargeAmount = serviceChargeAmount;
    }

    public BigDecimal getGrandTotal() {
        return grandTotal;
    }

    public void setGrandTotal(BigDecimal grandTotal) {
        this.grandTotal = grandTotal;
    }

    public List<ReceiptLineResponse> getLines() {
        return lines;
    }

    public void setLines(List<ReceiptLineResponse> lines) {
        this.lines = lines;
    }
}
