package com.poautomation.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String poNumber;
    private String supplier;
    private String brand;
    private String buyer;

    private String department;
    private String category;
    private String styleNumber;
    private Integer totalUnits;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String currency;

    private String country;
    private String mode;
    private String portOfLoading;
    private String sampleApprovedStatus;

    // Supplier/Factory details
    private String supplierLocation;
    private String factoryName;
    private String factoryLocation;

    // Excel template "UK PO RECD DATE"
    private LocalDate dateOrderPlaced;
    private LocalDate revisedExFactoryDate;
    private LocalDate confirmedExFactoryDate;
    private LocalDate actualDeliveryDate;
    private String deliveryStatus;
    private String sourceFileName;
    private String parseStatus;

    @Column(length = 4000)
    private String parseErrors;

    @Column(length = 1200)
    private String rawTextExcerpt;

    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public String getPoNumber() { return poNumber; }

    public void setPoNumber(String poNumber) { this.poNumber = poNumber; }

    public String getSupplier() { return supplier; }

    public void setSupplier(String supplier) { this.supplier = supplier; }

    public String getBrand() { return brand; }

    public void setBrand(String brand) { this.brand = brand; }

    public String getBuyer() { return buyer; }

    public void setBuyer(String buyer) { this.buyer = buyer; }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStyleNumber() {
        return styleNumber;
    }

    public void setStyleNumber(String styleNumber) {
        this.styleNumber = styleNumber;
    }

    public Integer getTotalUnits() { return totalUnits; }

    public void setTotalUnits(Integer totalUnits) { this.totalUnits = totalUnits; }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTotalAmount() { return totalAmount; }

    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getCurrency() { return currency; }

    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getConfirmedExFactoryDate() {
        return confirmedExFactoryDate;
    }

    public void setConfirmedExFactoryDate(LocalDate confirmedExFactoryDate) {
        this.confirmedExFactoryDate = confirmedExFactoryDate;
    }

    public LocalDate getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(LocalDate actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getSourceFileName() {
        return sourceFileName;
    }

    public void setSourceFileName(String sourceFileName) {
        this.sourceFileName = sourceFileName;
    }

    public String getParseStatus() {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus) {
        this.parseStatus = parseStatus;
    }

    public String getParseErrors() {
        return parseErrors;
    }

    public void setParseErrors(String parseErrors) {
        this.parseErrors = parseErrors;
    }

    public String getRawTextExcerpt() {
        return rawTextExcerpt;
    }

    public void setRawTextExcerpt(String rawTextExcerpt) {
        this.rawTextExcerpt = rawTextExcerpt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PurchaseOrderLine> getLines() {
        return lines;
    }

    public void setLines(List<PurchaseOrderLine> lines) {
        this.lines = lines;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPortOfLoading() {
        return portOfLoading;
    }

    public void setPortOfLoading(String portOfLoading) {
        this.portOfLoading = portOfLoading;
    }

    public String getSampleApprovedStatus() {
        return sampleApprovedStatus;
    }

    public void setSampleApprovedStatus(String sampleApprovedStatus) {
        this.sampleApprovedStatus = sampleApprovedStatus;
    }

    public String getSupplierLocation() {
        return supplierLocation;
    }

    public void setSupplierLocation(String supplierLocation) {
        this.supplierLocation = supplierLocation;
    }

    public String getFactoryName() {
        return factoryName;
    }

    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName;
    }

    public String getFactoryLocation() {
        return factoryLocation;
    }

    public void setFactoryLocation(String factoryLocation) {
        this.factoryLocation = factoryLocation;
    }

    public LocalDate getDateOrderPlaced() {
        return dateOrderPlaced;
    }

    public void setDateOrderPlaced(LocalDate dateOrderPlaced) {
        this.dateOrderPlaced = dateOrderPlaced;
    }

    public LocalDate getRevisedExFactoryDate() {
        return revisedExFactoryDate;
    }

    public void setRevisedExFactoryDate(LocalDate revisedExFactoryDate) {
        this.revisedExFactoryDate = revisedExFactoryDate;
    }

    /**
     * Recalculate header totals from line items (used for KPI dashboards).
     */
    public void recalcTotals() {
        if (lines == null || lines.isEmpty()) {
            this.totalUnits = 0;
            this.totalAmount = BigDecimal.ZERO;
            return;
        }

        int units = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseOrderLine line : lines) {
            if (line.getTotalOrderQty() != null) {
                units += line.getTotalOrderQty();
            }
            // Prefer GBP totals if present, otherwise USD.
            if (line.getGbpTotalPoValue() != null) {
                total = total.add(line.getGbpTotalPoValue());
            } else if (line.getUsdTotalPoValue() != null) {
                total = total.add(line.getUsdTotalPoValue());
            }
        }

        this.totalUnits = units;
        this.totalAmount = total;
    }
}