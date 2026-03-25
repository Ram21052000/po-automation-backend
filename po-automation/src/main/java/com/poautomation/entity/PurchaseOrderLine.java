package com.poautomation.entity;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_lines")
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id")
    @JsonIgnore
    private PurchaseOrder purchaseOrder;

    // Excel template columns (line-level)
    private Integer srNo; // SR. NO.
    private String styleNo; // STYLE NO
    private String newOrRebuy; // NEW/REBUY
    private String color; // COLOR
    private String supplierRefNo; // Supplier Ref. No.
    @Column(length = 4000)
    private String productDescription; // Product Description

    private Integer totalOrderQty; // TOTAL ORDER QTY

    // Prices/values in both currencies (one side can be null)
    private BigDecimal usdPricePerPc;
    private BigDecimal gbpPricePerPc;

    private BigDecimal usdTotalPoValue;
    private BigDecimal gbpTotalPoValue;

    private String sustainable; // N/Y

    public Long getId() {
        return id;
    }

    public PurchaseOrder getPurchaseOrder() {
        return purchaseOrder;
    }

    public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    public Integer getSrNo() {
        return srNo;
    }

    public void setSrNo(Integer srNo) {
        this.srNo = srNo;
    }

    public String getStyleNo() {
        return styleNo;
    }

    public void setStyleNo(String styleNo) {
        this.styleNo = styleNo;
    }

    public String getNewOrRebuy() {
        return newOrRebuy;
    }

    public void setNewOrRebuy(String newOrRebuy) {
        this.newOrRebuy = newOrRebuy;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSupplierRefNo() {
        return supplierRefNo;
    }

    public void setSupplierRefNo(String supplierRefNo) {
        this.supplierRefNo = supplierRefNo;
    }

    public String getProductDescription() {
        return productDescription;
    }

    public void setProductDescription(String productDescription) {
        this.productDescription = productDescription;
    }

    public Integer getTotalOrderQty() {
        return totalOrderQty;
    }

    public void setTotalOrderQty(Integer totalOrderQty) {
        this.totalOrderQty = totalOrderQty;
    }

    public BigDecimal getUsdPricePerPc() {
        return usdPricePerPc;
    }

    public void setUsdPricePerPc(BigDecimal usdPricePerPc) {
        this.usdPricePerPc = usdPricePerPc;
    }

    public BigDecimal getGbpPricePerPc() {
        return gbpPricePerPc;
    }

    public void setGbpPricePerPc(BigDecimal gbpPricePerPc) {
        this.gbpPricePerPc = gbpPricePerPc;
    }

    public BigDecimal getUsdTotalPoValue() {
        return usdTotalPoValue;
    }

    public void setUsdTotalPoValue(BigDecimal usdTotalPoValue) {
        this.usdTotalPoValue = usdTotalPoValue;
    }

    public BigDecimal getGbpTotalPoValue() {
        return gbpTotalPoValue;
    }

    public void setGbpTotalPoValue(BigDecimal gbpTotalPoValue) {
        this.gbpTotalPoValue = gbpTotalPoValue;
    }

    public String getSustainable() {
        return sustainable;
    }

    public void setSustainable(String sustainable) {
        this.sustainable = sustainable;
    }
}

