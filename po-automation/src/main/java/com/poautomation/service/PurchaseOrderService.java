package com.poautomation.service;

import com.poautomation.entity.PurchaseOrder;
import com.poautomation.repository.PurchaseOrderRepository;
import com.poautomation.util.PdfParserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    @Autowired
    private PdfParserUtil parser;

    @Autowired
    private PurchaseOrderRepository repo;

    public PurchaseOrder upload(MultipartFile file) {
        try {
            PurchaseOrder po = parser.parse(file);
            return repo.save(po);
        } catch (Exception e) {
            throw new RuntimeException("Error processing PDF: " + e.getMessage());
        }
    }

    public List<PurchaseOrder> getAll(LocalDate startDate, LocalDate endDate, String supplier, String buyer, String category) {
        Specification<PurchaseOrder> spec = Specification.where(null);

        if (startDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateOrderPlaced"), startDate));
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateOrderPlaced"), endDate));
        }
        if (hasText(supplier)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("supplier")), supplier.toLowerCase()));
        }
        if (hasText(buyer)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("buyer")), buyer.toLowerCase()));
        }
        if (hasText(category)) {
            spec = spec.and((root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.toLowerCase()));
        }

        return repo.findAll(spec);
    }

    public List<Map<String, Object>> getSupplierBrandInsights() {
        List<Map<String, Object>> output = new ArrayList<>();
        for (Object[] row : repo.getSupplierBrandInsights()) {
            output.add(Map.of(
                    "supplier", asString(row[0]),
                    "brand", asString(row[1]),
                    "orderCount", row[2],
                    "totalUnits", row[3],
                    "totalAmount", row[4]
            ));
        }
        return output;
    }

    public List<Map<String, Object>> getDeliveryTimeline() {
        List<Map<String, Object>> output = new ArrayList<>();
        for (Object[] row : repo.getDeliveryTimeline()) {
            output.add(Map.of(
                    "poNumber", asString(row[0]),
                    "confirmedExFactoryDate", row[1],
                    "actualDeliveryDate", row[2],
                    "deliveryStatus", asString(row[3])
            ));
        }
        return output;
    }

    public InputStream exportCsv() {
        List<PurchaseOrder> records = repo.findAll();
        String header = "id,poNumber,supplier,brand,buyer,category,styleNumber,totalUnits,unitPrice,totalAmount,currency,confirmedExFactoryDate,actualDeliveryDate,deliveryStatus,parseStatus\n";
        String rows = records.stream()
                .map(this::toCsvRow)
                .collect(Collectors.joining("\n"));
        return new ByteArrayInputStream((header + rows).getBytes(StandardCharsets.UTF_8));
    }

    @Transactional(readOnly = true)
    public InputStream exportXlsx() {
        List<PurchaseOrder> records = repo.findAll();

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("PO Export");

            // Template columns (from your provided Excel header row)
            String[] headers = new String[]{
                    "SR. NO.", "SUPPLIER", "BRAND", "BUYER NAME", "DEPARTMENT", "CATEGORY", "STYLE NO", "NEW/REBUY",
                    "COLOR", "PO NO", "COUNTRY", "Supplier Ref. No.", "Product Description", "UK PO RECD DATE",
                    "TOTAL ORDER QTY", "USD PRICE PER PC", "GBP PRICE PER PC", "USD TOTAL PO VALUE", "GBP TOTAL PO VALUE",
                    "CONFIRMED EX-FACTORY", "REVISED EX- FACTORY", "Delivery Date", "MODE", "Port of load",
                    "Sample approved status", "Sustainable"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
            }

            DataFormat df = workbook.createDataFormat();
            CellStyle usdStyle = workbook.createCellStyle();
            usdStyle.setDataFormat(df.getFormat("\"$\"#,##0.00"));
            CellStyle gbpStyle = workbook.createCellStyle();
            gbpStyle.setDataFormat(df.getFormat("\"£\"#,##0.00"));
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(df.getFormat("#,##0.00"));

            int rowIdx = 1;
            for (PurchaseOrder po : records) {
                if (po.getLines() == null || po.getLines().isEmpty()) continue;

                for (var line : po.getLines()) {
                    Row r = sheet.createRow(rowIdx++);
                    int col = 0;

                    setInt(r, col++, line.getSrNo());
                    setString(r, col++, po.getSupplier());
                    setString(r, col++, po.getBrand());
                    setString(r, col++, po.getBuyer());
                    setString(r, col++, po.getDepartment());
                    setString(r, col++, po.getCategory());
                    setString(r, col++, line.getStyleNo());
                    setString(r, col++, line.getNewOrRebuy());
                    setString(r, col++, line.getColor());
                    setString(r, col++, po.getPoNumber());
                    setString(r, col++, po.getCountry());
                    setString(r, col++, line.getSupplierRefNo());
                    setString(r, col++, line.getProductDescription());

                    // Dates: keep as strings to avoid locale/date system mismatch in template.
                    setString(r, col++, po.getDateOrderPlaced() == null ? null : po.getDateOrderPlaced().toString());
                    setInt(r, col++, line.getTotalOrderQty());

                    col = setMoney(r, col++, line.getUsdPricePerPc(), usdStyle);
                    col = setMoney(r, col++, line.getGbpPricePerPc(), gbpStyle);
                    col = setMoney(r, col++, line.getUsdTotalPoValue(), usdStyle);
                    col = setMoney(r, col++, line.getGbpTotalPoValue(), gbpStyle);

                    setString(r, col++, po.getConfirmedExFactoryDate() == null ? null : po.getConfirmedExFactoryDate().toString());
                    setString(r, col++, po.getRevisedExFactoryDate() == null ? null : po.getRevisedExFactoryDate().toString());
                    setString(r, col++, po.getActualDeliveryDate() == null ? null : po.getActualDeliveryDate().toString());
                    setString(r, col++, po.getMode());
                    setString(r, col++, po.getPortOfLoading());
                    setString(r, col++, po.getSampleApprovedStatus());
                    setString(r, col++, line.getSustainable());
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return new ByteArrayInputStream(bos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Excel export failed: " + e.getMessage(), e);
        }
    }

    private void setString(Row r, int col, String value) {
        Cell c = r.createCell(col);
        if (value == null) return;
        c.setCellValue(value);
    }

    private void setInt(Row r, int col, Integer value) {
        Cell c = r.createCell(col);
        if (value == null) return;
        c.setCellValue(value);
    }

    private int setMoney(Row r, int col, BigDecimal value, CellStyle style) {
        Cell c = r.createCell(col);
        if (value == null) return col + 1;
        c.setCellValue(value.doubleValue());
        if (style != null) c.setCellStyle(style);
        return col + 1;
    }

    public Map<String, Object> getKpis() {
        List<PurchaseOrder> list = repo.findAll();
        int totalOrders = list.size();
        int totalUnits = list.stream().map(PurchaseOrder::getTotalUnits).filter(v -> v != null).mapToInt(Integer::intValue).sum();
        BigDecimal totalValue = list.stream()
                .map(PurchaseOrder::getTotalAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long delayed = list.stream().filter(po -> "DELAYED".equalsIgnoreCase(po.getDeliveryStatus())).count();
        return Map.of(
                "totalOrders", totalOrders,
                "totalUnits", totalUnits,
                "totalValue", totalValue,
                "delayedOrders", delayed
        );
    }

    private String toCsvRow(PurchaseOrder po) {
        return String.join(",",
                csv(po.getId()),
                csv(po.getPoNumber()),
                csv(po.getSupplier()),
                csv(po.getBrand()),
                csv(po.getBuyer()),
                csv(po.getCategory()),
                csv(po.getStyleNumber()),
                csv(po.getTotalUnits()),
                csv(po.getUnitPrice()),
                csv(po.getTotalAmount()),
                csv(po.getCurrency()),
                csv(po.getConfirmedExFactoryDate()),
                csv(po.getActualDeliveryDate()),
                csv(po.getDeliveryStatus()),
                csv(po.getParseStatus()));
    }

    private String csv(Object value) {
        String raw = value == null ? "" : value.toString();
        return "\"" + raw.replace("\"", "\"\"") + "\"";
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String asString(Object value) {
        return value == null ? "" : value.toString();
    }
}