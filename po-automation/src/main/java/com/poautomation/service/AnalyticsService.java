package com.poautomation.service;

import com.poautomation.entity.PurchaseOrder;
import com.poautomation.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.WeekFields;
import java.util.*;

@Service
public class AnalyticsService {

    @Autowired
    private PurchaseOrderRepository repo;

    @Autowired
    private CurrencyService currencyService;

    // Fiscal year: Sep -> Aug
    public int fiscalYear(LocalDate date) {
        if (date == null) return 0;
        return date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> countryWiseBusiness() {
        List<String> allowed = List.of("India", "Pakistan", "Bangladesh", "China");
        Set<String> allowedSet = new HashSet<>(allowed);

        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();

        Map<String, Map<String, Object>> out = new LinkedHashMap<>();
        for (String c : allowed) {
            out.put(c, new HashMap<>(Map.of(
                    "country", c,
                    "totalGbp", BigDecimal.ZERO,
                    "totalUsd", BigDecimal.ZERO,
                    "totalUsdConverted", BigDecimal.ZERO
            )));
        }

        for (PurchaseOrder po : repo.findAll()) {
            String country = Optional.ofNullable(po.getCountry()).orElse("N/A");
            if (!allowedSet.contains(country)) continue;
            if (po.getLines() == null) continue;

            BigDecimal gbp = BigDecimal.ZERO;
            BigDecimal usd = BigDecimal.ZERO;
            BigDecimal usdConverted = BigDecimal.ZERO;

            for (var line : po.getLines()) {
                if (line.getGbpTotalPoValue() != null) {
                    gbp = gbp.add(line.getGbpTotalPoValue());
                    usdConverted = usdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                }
                if (line.getUsdTotalPoValue() != null) {
                    usd = usd.add(line.getUsdTotalPoValue());
                    usdConverted = usdConverted.add(line.getUsdTotalPoValue());
                }
            }

            Map<String, Object> agg = out.get(country);
            agg.put("totalGbp", ((BigDecimal) agg.get("totalGbp")).add(gbp));
            agg.put("totalUsd", ((BigDecimal) agg.get("totalUsd")).add(usd));
            agg.put("totalUsdConverted", ((BigDecimal) agg.get("totalUsdConverted")).add(usdConverted));
        }

        return Map.of("countryTotals", new ArrayList<>(out.values()));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> timeSummaries(String granularity) {
        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();
        List<PurchaseOrder> all = repo.findAll();

        // key -> aggregated metrics
        Map<String, Map<String, Object>> totals = new LinkedHashMap<>();
        WeekFields wf = WeekFields.ISO;

        for (PurchaseOrder po : all) {
            LocalDate d = Optional.ofNullable(po.getDateOrderPlaced()).orElse(po.getConfirmedExFactoryDate());
            if (d == null) continue;
            String key;
            if ("week".equalsIgnoreCase(granularity)) {
                key = fiscalYear(d) + "-W" + String.format("%02d", d.get(wf.weekOfWeekBasedYear()));
            } else if ("month".equalsIgnoreCase(granularity)) {
                Month m = d.getMonth();
                key = fiscalYear(d) + "-" + String.format("%02d", m.getValue());
            } else {
                key = String.valueOf(fiscalYear(d));
            }

            totals.computeIfAbsent(key, k -> new HashMap<>(Map.of(
                    "key", k,
                    "poCount", 0,
                    "totalQty", 0,
                    "totalUsd", BigDecimal.ZERO,
                    "totalGbp", BigDecimal.ZERO,
                    "totalUsdConverted", BigDecimal.ZERO
            )));

            Map<String, Object> agg = totals.get(key);
            agg.put("poCount", ((Integer) agg.get("poCount")) + 1);

            if (po.getLines() != null) {
                int qty = (Integer) agg.get("totalQty");
                BigDecimal totalUsd = (BigDecimal) agg.get("totalUsd");
                BigDecimal totalGbp = (BigDecimal) agg.get("totalGbp");
                BigDecimal totalUsdConverted = (BigDecimal) agg.get("totalUsdConverted");

                for (var line : po.getLines()) {
                    if (line.getTotalOrderQty() != null) qty += line.getTotalOrderQty();
                    if (line.getUsdTotalPoValue() != null) totalUsd = totalUsd.add(line.getUsdTotalPoValue());
                    if (line.getGbpTotalPoValue() != null) totalGbp = totalGbp.add(line.getGbpTotalPoValue());
                    if (line.getGbpTotalPoValue() != null) totalUsdConverted = totalUsdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                    if (line.getUsdTotalPoValue() != null) totalUsdConverted = totalUsdConverted.add(line.getUsdTotalPoValue());
                }

                agg.put("totalQty", qty);
                agg.put("totalUsd", totalUsd);
                agg.put("totalGbp", totalGbp);
                agg.put("totalUsdConverted", totalUsdConverted);
            }
        }

        return Map.of("granularity", granularity, "totals", totals);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> brandLinks() {
        List<PurchaseOrder> all = repo.findAll();
        Map<String, Set<String>> brandToSuppliers = new HashMap<>();
        Map<String, Set<String>> brandToFactories = new HashMap<>();
        Map<String, BigDecimal> brandToUsdConverted = new HashMap<>();

        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();

        for (PurchaseOrder po : all) {
            String brand = Optional.ofNullable(po.getBrand()).orElse("N/A");
            brandToSuppliers.computeIfAbsent(brand, k -> new HashSet<>()).add(Optional.ofNullable(po.getSupplier()).orElse("N/A"));
            brandToFactories.computeIfAbsent(brand, k -> new HashSet<>()).add(Optional.ofNullable(po.getFactoryName()).orElse("N/A"));

            if (po.getLines() != null) {
                BigDecimal usdConverted = BigDecimal.ZERO;
                for (var line : po.getLines()) {
                    if (line.getUsdTotalPoValue() != null) usdConverted = usdConverted.add(line.getUsdTotalPoValue());
                    if (line.getGbpTotalPoValue() != null) usdConverted = usdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                }
                brandToUsdConverted.put(brand, brandToUsdConverted.getOrDefault(brand, BigDecimal.ZERO).add(usdConverted));
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String brand : brandToSuppliers.keySet()) {
            rows.add(Map.of(
                    "brand", brand,
                    "suppliers", brandToSuppliers.get(brand).size(),
                    "factories", brandToFactories.getOrDefault(brand, Collections.emptySet()).size(),
                    "totalUsdConverted", brandToUsdConverted.getOrDefault(brand, BigDecimal.ZERO)
            ));
        }
        return Map.of("brandLinks", rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> supplierLinks() {
        List<PurchaseOrder> all = repo.findAll();
        Map<String, Set<String>> supplierToFactories = new HashMap<>();
        Map<String, Set<String>> supplierToBrands = new HashMap<>();
        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();
        Map<String, BigDecimal> supplierToUsdConverted = new HashMap<>();

        for (PurchaseOrder po : all) {
            String supplier = Optional.ofNullable(po.getSupplier()).orElse("N/A");
            supplierToFactories.computeIfAbsent(supplier, k -> new HashSet<>()).add(Optional.ofNullable(po.getFactoryName()).orElse("N/A"));
            supplierToBrands.computeIfAbsent(supplier, k -> new HashSet<>()).add(Optional.ofNullable(po.getBrand()).orElse("N/A"));

            if (po.getLines() != null) {
                BigDecimal usdConverted = BigDecimal.ZERO;
                for (var line : po.getLines()) {
                    if (line.getUsdTotalPoValue() != null) usdConverted = usdConverted.add(line.getUsdTotalPoValue());
                    if (line.getGbpTotalPoValue() != null) usdConverted = usdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                }
                supplierToUsdConverted.put(supplier, supplierToUsdConverted.getOrDefault(supplier, BigDecimal.ZERO).add(usdConverted));
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String supplier : supplierToFactories.keySet()) {
            rows.add(Map.of(
                    "supplier", supplier,
                    "factories", supplierToFactories.get(supplier).size(),
                    "brands", supplierToBrands.getOrDefault(supplier, Collections.emptySet()).size(),
                    "totalUsdConverted", supplierToUsdConverted.getOrDefault(supplier, BigDecimal.ZERO)
            ));
        }
        return Map.of("supplierLinks", rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> factoryLinks() {
        List<PurchaseOrder> all = repo.findAll();
        Map<String, Set<String>> factoryToSuppliers = new HashMap<>();
        Map<String, Set<String>> factoryToBrands = new HashMap<>();
        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();
        Map<String, BigDecimal> factoryToUsdConverted = new HashMap<>();

        for (PurchaseOrder po : all) {
            String factory = Optional.ofNullable(po.getFactoryName()).orElse("N/A");
            factoryToSuppliers.computeIfAbsent(factory, k -> new HashSet<>()).add(Optional.ofNullable(po.getSupplier()).orElse("N/A"));
            factoryToBrands.computeIfAbsent(factory, k -> new HashSet<>()).add(Optional.ofNullable(po.getBrand()).orElse("N/A"));

            if (po.getLines() != null) {
                BigDecimal usdConverted = BigDecimal.ZERO;
                for (var line : po.getLines()) {
                    if (line.getUsdTotalPoValue() != null) usdConverted = usdConverted.add(line.getUsdTotalPoValue());
                    if (line.getGbpTotalPoValue() != null) usdConverted = usdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                }
                factoryToUsdConverted.put(factory, factoryToUsdConverted.getOrDefault(factory, BigDecimal.ZERO).add(usdConverted));
            }
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String factory : factoryToSuppliers.keySet()) {
            rows.add(Map.of(
                    "factory", factory,
                    "suppliers", factoryToSuppliers.get(factory).size(),
                    "brands", factoryToBrands.getOrDefault(factory, Collections.emptySet()).size(),
                    "totalUsdConverted", factoryToUsdConverted.getOrDefault(factory, BigDecimal.ZERO)
            ));
        }
        return Map.of("factoryLinks", rows);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> overallTotals() {
        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();
        List<PurchaseOrder> all = repo.findAll();

        int poCount = all.size();
        int totalQty = 0;
        BigDecimal totalUsd = BigDecimal.ZERO;
        BigDecimal totalGbp = BigDecimal.ZERO;
        BigDecimal totalUsdConverted = BigDecimal.ZERO;

        for (PurchaseOrder po : all) {
            if (po.getLines() == null) continue;
            for (var line : po.getLines()) {
                if (line.getTotalOrderQty() != null) totalQty += line.getTotalOrderQty();
                if (line.getUsdTotalPoValue() != null) {
                    totalUsd = totalUsd.add(line.getUsdTotalPoValue());
                    totalUsdConverted = totalUsdConverted.add(line.getUsdTotalPoValue());
                }
                if (line.getGbpTotalPoValue() != null) {
                    totalGbp = totalGbp.add(line.getGbpTotalPoValue());
                    totalUsdConverted = totalUsdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                }
            }
        }

        return Map.of(
                "poCount", poCount,
                "totalQty", totalQty,
                "totalUsd", totalUsd,
                "totalGbp", totalGbp,
                "totalUsdConverted", totalUsdConverted
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> supplierDetails() {
        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();
        List<PurchaseOrder> all = repo.findAll();

        Map<String, Map<String, Object>> temp = new HashMap<>();

        for (PurchaseOrder po : all) {
            String supplier = Optional.ofNullable(po.getSupplier()).orElse("N/A");
            temp.computeIfAbsent(supplier, s -> new HashMap<>(Map.of(
                    "supplier", s,
                    "supplierLocation", Optional.ofNullable(po.getSupplierLocation()).orElse(""),
                    "factoryCount", 0,
                    "brandCount", 0,
                    "totalUsdConverted", BigDecimal.ZERO
            )));

            Map<String, Object> agg = temp.get(supplier);
            // counts: use sets on the fly by recalculating quickly
            // (prototype simplicity: recompute per supplier from all POs)
        }

        // recompute with sets so counts are accurate
        for (var entry : temp.entrySet()) {
            String supplier = entry.getKey();
            Set<String> factories = new HashSet<>();
            Set<String> brands = new HashSet<>();
            BigDecimal totalUsdConverted = BigDecimal.ZERO;

            for (PurchaseOrder po : all) {
                if (!supplier.equals(po.getSupplier())) continue;
                if (po.getFactoryName() != null) factories.add(po.getFactoryName());
                if (po.getBrand() != null) brands.add(po.getBrand());
                if (po.getLines() != null) {
                    for (var line : po.getLines()) {
                        if (line.getUsdTotalPoValue() != null) totalUsdConverted = totalUsdConverted.add(line.getUsdTotalPoValue());
                        if (line.getGbpTotalPoValue() != null) totalUsdConverted = totalUsdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                    }
                }
            }

            entry.getValue().put("factoryCount", factories.size());
            entry.getValue().put("brandCount", brands.size());
            entry.getValue().put("totalUsdConverted", totalUsdConverted);
        }

        return new ArrayList<>(temp.values());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> factoryDetails() {
        BigDecimal gbpToUsdAdjustedRate = currencyService.getGbpToUsdAdjustedRate();
        List<PurchaseOrder> all = repo.findAll();

        Map<String, Map<String, Object>> temp = new HashMap<>();
        for (PurchaseOrder po : all) {
            String factory = Optional.ofNullable(po.getFactoryName()).orElse("N/A");
            temp.computeIfAbsent(factory, f -> new HashMap<>(Map.of(
                    "factory", f,
                    "factoryLocation", Optional.ofNullable(po.getFactoryLocation()).orElse(""),
                    "supplierCount", 0,
                    "brandCount", 0,
                    "totalUsdConverted", BigDecimal.ZERO
            )));
        }

        for (var entry : temp.entrySet()) {
            String factory = entry.getKey();
            Set<String> suppliers = new HashSet<>();
            Set<String> brands = new HashSet<>();
            BigDecimal totalUsdConverted = BigDecimal.ZERO;

            for (PurchaseOrder po : all) {
                if (!factory.equals(po.getFactoryName())) continue;
                if (po.getSupplier() != null) suppliers.add(po.getSupplier());
                if (po.getBrand() != null) brands.add(po.getBrand());
                if (po.getLines() != null) {
                    for (var line : po.getLines()) {
                        if (line.getUsdTotalPoValue() != null) totalUsdConverted = totalUsdConverted.add(line.getUsdTotalPoValue());
                        if (line.getGbpTotalPoValue() != null) totalUsdConverted = totalUsdConverted.add(line.getGbpTotalPoValue().multiply(gbpToUsdAdjustedRate));
                    }
                }
            }

            entry.getValue().put("supplierCount", suppliers.size());
            entry.getValue().put("brandCount", brands.size());
            entry.getValue().put("totalUsdConverted", totalUsdConverted);
        }

        return new ArrayList<>(temp.values());
    }
}

