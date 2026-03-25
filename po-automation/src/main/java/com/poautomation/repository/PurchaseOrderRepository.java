package com.poautomation.repository;

import com.poautomation.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    @Query("""
            SELECT p.supplier, p.brand, COUNT(p), COALESCE(SUM(p.totalUnits), 0), COALESCE(SUM(p.totalAmount), 0)
            FROM PurchaseOrder p
            GROUP BY p.supplier, p.brand
            ORDER BY COALESCE(SUM(p.totalAmount), 0) DESC
            """)
    List<Object[]> getSupplierBrandInsights();

    @Query("""
            SELECT p.poNumber, p.confirmedExFactoryDate, p.actualDeliveryDate, p.deliveryStatus
            FROM PurchaseOrder p
            WHERE p.confirmedExFactoryDate IS NOT NULL OR p.actualDeliveryDate IS NOT NULL
            ORDER BY p.createdAt DESC
            """)
    List<Object[]> getDeliveryTimeline();
}
