package com.poautomation.util;

import com.poautomation.entity.PurchaseOrder;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class PdfParserUtil {
    private static final DateTimeFormatter[] DATE_FORMATS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    };

    public PurchaseOrder parse(MultipartFile file) throws Exception {
        String text;
        try (PDDocument doc = PDDocument.load(file.getInputStream())) {
            text = new PDFTextStripper().getText(doc);
        }

        List<String> errors = new ArrayList<>();
        PurchaseOrder po = new PurchaseOrder();
        po.setSourceFileName(file.getOriginalFilename());
        po.setRawTextExcerpt(text.substring(0, Math.min(1000, text.length())));

        po.setPoNumber(extractByKeys(text, "Purchase Order No", "PO Number", "PO No"));
        po.setSupplier(extractSupplierName(text));
        po.setBrand(detectBrand(text));
        po.setBuyer(extractByKeys(text, "Buyer", "Buyer Name"));
        po.setCountry(extractByKeys(text, "Destination Country", "Country"));
        po.setMode(extractByKeys(text, "Incoterms", "Mode"));
        po.setPortOfLoading(extractByKeys(text, "Port of Loading", "Port of load", "Port of load"));
        po.setSampleApprovedStatus(extractByKeys(text, "Sample Approval Status", "Sample approved status", "Sample Approval Status"));

        po.setDateOrderPlaced(parseDate(extractByKeys(text, "Date Order Placed", "Date order placed", "UK PO RECD DATE")));
        po.setConfirmedExFactoryDate(parseDate(extractByKeys(text, "Ex Factory Date", "Confirmed Ex-Factory Date", "Confirmed Ex Factory Date")));
        po.setRevisedExFactoryDate(parseDate(extractByKeys(text, "Revised Ex- Factory", "Revised Ex Factory Date", "Revised Ex-Factory Date")));
        po.setActualDeliveryDate(parseDate(extractByKeys(text, "Delivery Date", "Actual Delivery Date")));

        // Currency: infer from the "All Amounts in ..." line when present.
        po.setCurrency(detectCurrency(text, po.getConfirmedExFactoryDate()));

        po.setLines(parseLines(text, errors, po.getCurrency()));
        if (po.getLines() != null) {
            po.getLines().forEach(l -> l.setPurchaseOrder(po));
        }
        po.recalcTotals();

        po.setDeliveryStatus(calculateDeliveryStatus(po.getConfirmedExFactoryDate(), po.getActualDeliveryDate()));

        validateRequired(po, errors);
        po.setParseStatus(errors.isEmpty() ? "SUCCESS" : "PARTIAL");
        po.setParseErrors(errors.isEmpty() ? null : String.join("; ", errors));
        return po;
    }

    private void validateRequired(PurchaseOrder po, List<String> errors) {
        if (blank(po.getPoNumber())) {
            errors.add("PO number missing");
        }
        // Many PDFs omit a clean "Supplier" header in extracted text; we allow missing supplier for prototype.
        if (blank(po.getBuyer())) {
            errors.add("Buyer missing");
        }
        if (blank(po.getCurrency())) errors.add("Currency missing");
    }

    private String calculateDeliveryStatus(LocalDate exFactory, LocalDate actualDelivery) {
        if (exFactory == null || actualDelivery == null) {
            return "UNKNOWN";
        }
        if (actualDelivery.isAfter(exFactory)) {
            return "DELAYED";
        }
        if (actualDelivery.isBefore(exFactory)) {
            return "EARLY";
        }
        return "ON_TIME";
    }

    private String extractByKeys(String text, String... keys) {
        for (String key : keys) {
            String value = extractAfterLabel(text, key);
            if (!blank(value)) {
                return value;
            }
        }
        return "";
    }

    private String extractAfterLabel(String text, String label) {
        String quotedLabel = Pattern.quote(label);
        Pattern p = Pattern.compile("(?im)^\\s*" + quotedLabel + "\\s*[:#\\-]?\\s*(.+)$");
        Matcher m = p.matcher(text);
        if (m.find()) {
            return clean(m.group(1));
        }

        Pattern fallback = Pattern.compile("(?is)" + quotedLabel + "\\s*[:#\\-]?\\s*([A-Za-z0-9/\\-., ]{1,80})");
        Matcher fallbackMatcher = fallback.matcher(text);
        if (fallbackMatcher.find()) {
            return clean(fallbackMatcher.group(1));
        }
        return "";
    }

    private Integer parseInteger(String value) {
        if (blank(value)) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        return Integer.parseInt(digits);
    }

    private BigDecimal parseDecimal(String value) {
        if (blank(value)) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9.\\-]", "");
        if (normalized.isEmpty() || "-".equals(normalized) || ".".equals(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String value) {
        if (blank(value)) {
            return null;
        }
        String candidate = value.trim();
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(candidate, format);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private String normalizeCurrency(String value) {
        String upper = Optional.ofNullable(value).orElse("").trim().toUpperCase(Locale.ENGLISH);
        if ("USD".equals(upper) || "GBP".equals(upper)) {
            return upper;
        }
        return upper.contains("GBP") ? "GBP" : "USD";
    }

    private String detectCurrency(String text, LocalDate exFactoryDate) {
        String upper = text == null ? "" : text.toUpperCase(Locale.ENGLISH);
        if (upper.contains("USD")) return "USD";
        if (upper.contains("GBP")) return "GBP";

        // Some PDFs may omit explicit USD/GBP labels in the extracted text; default to GBP for template matching.
        return "GBP";
    }

    private List<com.poautomation.entity.PurchaseOrderLine> parseLines(String text, List<String> errors, String currency) {
        List<com.poautomation.entity.PurchaseOrderLine> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        // Normalize line endings for easier parsing.
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");

        int tableHeaderIdx = -1;
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.contains("Size") && l.contains("EAN") && l.contains("Qty") && l.contains("Sustainable")) {
                tableHeaderIdx = i;
                break;
            }
        }

        if (tableHeaderIdx < 0) {
            errors.add("Line table header not found");
            return result;
        }

        // Everything after header until totals/footers should contain line items.
        int endIdx = lines.length;
        for (int i = tableHeaderIdx + 1; i < lines.length; i++) {
            String l = lines[i];
            if (l.toUpperCase(Locale.ENGLISH).contains("TOTAL UNITS BY COLOUR") ||
                    l.toUpperCase(Locale.ENGLISH).contains("TOTAL UNITS") ||
                    l.toUpperCase(Locale.ENGLISH).startsWith("DELIVER TO") ||
                    l.toUpperCase(Locale.ENGLISH).contains("DELIVER TO:")) {
                endIdx = i;
                break;
            }
        }

        // Parse blocks starting at each "line number" row.
        // Some PDFs render line number + style on the same line, while others put the number alone on its own line.
        Pattern startRowWithRemainder = Pattern.compile("^\\s*(\\d+)\\s+(.+)$");
        Pattern startRowDigitsOnly = Pattern.compile("^\\s*(\\d+)\\s*$");
        Integer currentLineNo = null;
        List<String> currentBlockLines = new ArrayList<>();

        for (int i = tableHeaderIdx + 1; i < endIdx; i++) {
            String l = lines[i].trim();
            if (l.isEmpty()) continue;

            Matcher m = startRowWithRemainder.matcher(l);
            Matcher digitsOnly = startRowDigitsOnly.matcher(l);
            if (m.find()) {
                // Flush previous block.
                if (currentLineNo != null && !currentBlockLines.isEmpty()) {
                    com.poautomation.entity.PurchaseOrderLine line = parseLineBlock(String.join("\n", currentBlockLines), currentLineNo, currency);
                    if (line != null) result.add(line);
                }

                currentLineNo = Integer.parseInt(m.group(1));
                currentBlockLines = new ArrayList<>();
                currentBlockLines.add(m.group(2));
            } else if (digitsOnly.find()) {
                // Flush previous block.
                if (currentLineNo != null && !currentBlockLines.isEmpty()) {
                    com.poautomation.entity.PurchaseOrderLine line = parseLineBlock(String.join("\n", currentBlockLines), currentLineNo, currency);
                    if (line != null) result.add(line);
                }

                currentLineNo = Integer.parseInt(digitsOnly.group(1));
                currentBlockLines = new ArrayList<>();
            } else if (currentLineNo != null) {
                currentBlockLines.add(l);
            }
        }

        // Flush last block.
        if (currentLineNo != null && !currentBlockLines.isEmpty()) {
            com.poautomation.entity.PurchaseOrderLine line = parseLineBlock(String.join("\n", currentBlockLines), currentLineNo, currency);
            if (line != null) result.add(line);
        }

        if (result.isEmpty()) {
            errors.add("No line items parsed");
        }
        return result;
    }

    private com.poautomation.entity.PurchaseOrderLine parseLineBlock(String blockText, Integer lineNo, String currency) {
        String normalized = blockText.replace("\r\n", " ").replace('\n', ' ').trim();
        if (normalized.isEmpty()) return null;

        // Tokenize: extracted PDF text is whitespace-separated.
        String[] rawTokens = normalized.split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String t : rawTokens) {
            if (!t.isBlank()) tokens.add(t);
        }
        if (tokens.size() < 10) return null;

        // Find "Each" anchor: Qty EAN ... Each <ppu> <net> <sustainable>
        int eachIdx = -1;
        for (int i = 0; i < tokens.size(); i++) {
            if ("Each".equalsIgnoreCase(tokens.get(i))) {
                eachIdx = i;
                break;
            }
        }
        if (eachIdx < 0 || eachIdx + 3 >= tokens.size()) return null;

        Integer qty = parseInteger(tokens.get(eachIdx - 1));
        String sustainableToken = tokens.get(eachIdx + 3);
        BigDecimal unitPrice = parseDecimal(tokens.get(eachIdx + 1));
        BigDecimal net = parseDecimal(tokens.get(eachIdx + 2));
        if (qty == null || unitPrice == null || net == null) return null;

        // The token before qty is typically EAN.
        String ean = eachIdx >= 2 ? tokens.get(eachIdx - 2) : null;
        Integer size = null;
        if (eachIdx >= 3) {
            size = parseInteger(tokens.get(eachIdx - 3));
        }

        // Attempt to extract a style/supplier ref and description:
        // - We'll assume style/supplier/description appear before EAN/size tokens.
        int sizeIdx = eachIdx - 3;
        int metaEnd = Math.max(0, sizeIdx);
        String productDescription = String.join(" ", tokens.subList(0, metaEnd));

        // Heuristic supplierRef: first token that looks like code (contains '/' or many digits)
        String supplierRefNo = "";
        int supplierRefIdx = -1;
        Pattern digits5 = Pattern.compile(".*\\d{5,}.*");
        for (int i = 0; i < metaEnd; i++) {
            String t = tokens.get(i);
            if (t.contains("/") || digits5.matcher(t).matches()) {
                supplierRefNo = t;
                supplierRefIdx = i;
                break;
            }
        }

        String styleNo = "";
        if (supplierRefIdx > 0) {
            styleNo = String.join("", tokens.subList(0, supplierRefIdx));
        } else if (!tokens.isEmpty()) {
            styleNo = tokens.get(0);
        }

        // Color: first known color word in productDescription.
        String color = detectColor(productDescription);

        com.poautomation.entity.PurchaseOrderLine line = new com.poautomation.entity.PurchaseOrderLine();
        line.setPurchaseOrder(null); // set by cascade through service
        line.setSrNo(lineNo);
        line.setStyleNo(styleNo);
        line.setSupplierRefNo(supplierRefNo);
        line.setProductDescription(productDescription);
        line.setColor(color);
        line.setTotalOrderQty(qty);
        if ("USD".equalsIgnoreCase(currency)) {
            line.setUsdPricePerPc(unitPrice);
            line.setUsdTotalPoValue(net);
        } else {
            line.setGbpPricePerPc(unitPrice);
            line.setGbpTotalPoValue(net);
        }
        if ("Y".equalsIgnoreCase(sustainableToken) || "N".equalsIgnoreCase(sustainableToken)) {
            line.setSustainable(sustainableToken.toUpperCase(Locale.ENGLISH));
        } else {
            line.setSustainable("N");
        }

        // NEW/REBUY: best effort from description tokens
        line.setNewOrRebuy(detectNewOrRebuy(tokens));
        return line;
    }

    private String detectNewOrRebuy(List<String> tokens) {
        for (String t : tokens) {
            if ("NEW".equalsIgnoreCase(t)) return "NEW";
            if ("REBUY".equalsIgnoreCase(t)) return "REBUY";
        }
        return "";
    }

    private String detectColor(String text) {
        if (text == null) return "";
        String t = text.toLowerCase(Locale.ENGLISH);
        String[] colors = new String[]{
                "black","white","navy","red","blue","green","pink","orange","purple","beige","ivory","brown","multi","yellow","mono","sage","steel",
                "champagne","dusty blue","light blue","khaki","light khaki","tan","champagne","chocolate","dusty","floral","ivory","sapphire"
        };
        for (String c : colors) {
            if (t.contains(c)) return c.toUpperCase(Locale.ENGLISH);
        }
        return "";
    }

    private String extractSupplierName(String text) {
        if (text == null || text.isBlank()) return "";
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n");
        for (int i = 0; i < lines.length - 1; i++) {
            String l = lines[i].trim();
            if (l.toLowerCase(Locale.ENGLISH).startsWith("supplier reference")) {
                // Supplier name is typically the next non-empty line.
                for (int j = i + 1; j < lines.length; j++) {
                    String next = lines[j].trim();
                    if (!next.isEmpty()) return next;
                }
            }
        }
        // Fallback: try to take "Supplier" header value if present.
        return extractByKeys(text, "Supplier", "Supplier Name");
    }

    private String detectBrand(String text) {
        if (text == null || text.isBlank()) return "";
        String upper = text.toUpperCase(Locale.ENGLISH);
        if (upper.contains("PRETTYLITTLETHING")) return "PRETTYLITTLETHING";
        if (upper.contains("BOOHOO.COM") || upper.contains("BOOHOO")) return "BOOHOO";
        if (upper.contains("COAST")) return "COAST";
        return extractByKeys(text, "Brand");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s{2,}", " ");
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }
}