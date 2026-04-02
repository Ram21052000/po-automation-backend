package com.poautomation.controller;

import com.poautomation.entity.PurchaseOrder;
import com.poautomation.repository.PurchaseOrderRepository;
import com.poautomation.util.PdfParserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/po")
public class FolderIngestController {

    @Autowired
    private PdfParserUtil parser;

    @Autowired
    private PurchaseOrderRepository repo;

    @PostMapping("/ingest-paths")
    public Map<String, Object> ingestPaths(@RequestBody Map<String, Object> payload) {
        Object pathsObj = payload.get("paths");
        if (!(pathsObj instanceof List<?> pathsList)) {
            return Map.of("ok", false, "error", "Expected {\"paths\": [\"C:\\\\...\", ...]}");
        }

        List<String> paths = pathsList.stream().map(String::valueOf).collect(Collectors.toList());
        List<String> errors = new ArrayList<>();
        int processed = 0;

        for (String p : paths) {
            try {
                Path path = Path.of(p);
                if (Files.isDirectory(path)) {
                    List<Path> pdfs = Files.walk(path)
                            .filter(f -> f.toString().toLowerCase().endsWith(".pdf"))
                            .collect(Collectors.toList());
                    for (Path pdf : pdfs) {
                        processed += ingestOne(pdf, errors);
                    }
                } else if (Files.isRegularFile(path) && p.toLowerCase().endsWith(".pdf")) {
                    processed += ingestOne(path, errors);
                }
            } catch (Exception e) {
                errors.add("Failed to ingest path " + p + ": " + e.getMessage());
            }
        }

        return Map.of(
                "ok", errors.isEmpty(),
                "processed", processed,
                "errors", errors
        );
    }

    private int ingestOne(Path pdf, List<String> errors) {
        try (InputStream is = Files.newInputStream(pdf)) {
            PurchaseOrder po = parser.parse(is, pdf.getFileName().toString());
            repo.save(po);
            return 1;
        } catch (Exception e) {
            errors.add("Failed to parse " + pdf + ": " + e.getMessage());
            return 0;
        }
    }
}

