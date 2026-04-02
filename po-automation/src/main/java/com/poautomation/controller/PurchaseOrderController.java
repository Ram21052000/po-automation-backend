package com.poautomation.controller;

import com.poautomation.entity.PurchaseOrder;
import com.poautomation.service.CurrencyService;
import com.poautomation.service.PurchaseOrderService;
import com.poautomation.service.AnalyticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/po")
public class PurchaseOrderController {

    @Autowired
    private PurchaseOrderService service;

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private AnalyticsService analyticsService;

    @PostMapping("/upload")
    public PurchaseOrder upload(@RequestParam("file") MultipartFile file) {
        return service.upload(file);
    }

    @GetMapping
    public List<PurchaseOrder> list(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String supplier,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String buyer,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String factoryName) {
        return service.getAll(startDate, endDate, country, supplier, buyer, brand, category, factoryName);
    }

    @GetMapping("/kpis")
    public Map<String, Object> kpis() {
        return service.getKpis();
    }

    @GetMapping("/insights/supplier-brand")
    public List<Map<String, Object>> supplierBrandInsights() {
        return service.getSupplierBrandInsights();
    }

    @GetMapping("/insights/delivery-timeline")
    public List<Map<String, Object>> deliveryTimeline() {
        return service.getDeliveryTimeline();
    }

    @GetMapping("/currency/usd-to-gbp")
    public Map<String, Object> usdToGbp() {
        return currencyService.getUsdToGbpRate();
    }

    @GetMapping("/currency/gbp-to-usd-adjusted")
    public Map<String, Object> gbpToUsdAdjusted() {
        return currencyService.getGbpToUsdAdjusted();
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCsv() {
        InputStreamResource file = new InputStreamResource(service.exportCsv());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase_orders.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(file);
    }

    @GetMapping("/export-xlsx")
    public ResponseEntity<InputStreamResource> exportXlsx() {
        InputStreamResource file = new InputStreamResource(service.exportXlsx());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase_orders.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(file);
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    // Analytics endpoints
    @GetMapping("/analytics/country")
    public Map<String, Object> countryTotals() {
        return analyticsService.countryWiseBusiness();
    }

    @GetMapping("/analytics/time")
    public Map<String, Object> timeTotals(@RequestParam(defaultValue = "month") String granularity) {
        return analyticsService.timeSummaries(granularity);
    }

    @GetMapping("/analytics/brand-links")
    public Map<String, Object> brandLinks() {
        return analyticsService.brandLinks();
    }

    @GetMapping("/analytics/supplier-links")
    public Map<String, Object> supplierLinks() {
        return analyticsService.supplierLinks();
    }

    @GetMapping("/analytics/factory-links")
    public Map<String, Object> factoryLinks() {
        return analyticsService.factoryLinks();
    }

    @GetMapping("/analytics/overall-totals-usd")
    public Map<String, Object> overallTotals() {
        return analyticsService.overallTotals();
    }

    @GetMapping("/analytics/suppliers")
    public List<Map<String, Object>> supplierDetails() {
        return analyticsService.supplierDetails();
    }

    @GetMapping("/analytics/factories")
    public List<Map<String, Object>> factoryDetails() {
        return analyticsService.factoryDetails();
    }
}