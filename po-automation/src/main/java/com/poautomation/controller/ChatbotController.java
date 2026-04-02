package com.poautomation.controller;

import com.poautomation.service.AnalyticsService;
import com.poautomation.service.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Map;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private AnalyticsService analyticsService;
    @Autowired
    private CurrencyService currencyService;

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> payload) {
        String q = payload.getOrDefault("q", "").toLowerCase(Locale.ENGLISH);

        if (q.contains("country")) {
            return Map.of("answer", "Country-wise business totals", "data", analyticsService.countryWiseBusiness());
        }
        if (q.contains("week")) {
            return Map.of("answer", "Week-wise totals (Sep–Aug FY)", "data", analyticsService.timeSummaries("week"));
        }
        if (q.contains("month")) {
            return Map.of("answer", "Month-wise totals (Sep–Aug FY)", "data", analyticsService.timeSummaries("month"));
        }
        if (q.contains("year")) {
            return Map.of("answer", "Year-wise totals (Sep–Aug FY)", "data", analyticsService.timeSummaries("year"));
        }
        if (q.contains("brand") && (q.contains("supplier") || q.contains("factory"))) {
            return Map.of("answer", "Brand links: suppliers and factories per brand", "data", analyticsService.brandLinks());
        }
        if ((q.contains("supplier") && q.contains("location")) || q.contains("supplier details")) {
            return Map.of("answer", "Supplier details: supplier name + location", "data", analyticsService.supplierDetails());
        }
        if ((q.contains("factory") && q.contains("location")) || q.contains("factory details")) {
            return Map.of("answer", "Factory details: factory name + location", "data", analyticsService.factoryDetails());
        }
        if (q.contains("supplier") && (q.contains("factory") || q.contains("brand"))) {
            return Map.of("answer", "Supplier links: factories and brands per supplier", "data", analyticsService.supplierLinks());
        }
        if (q.contains("factory") && (q.contains("supplier") || q.contains("brand"))) {
            return Map.of("answer", "Factory links: suppliers and brands per factory", "data", analyticsService.factoryLinks());
        }
        if (q.contains("convert") && (q.contains("business") || q.contains("entire"))) {
            return Map.of("answer", "Entire business converted into USD (adjusted GBP→USD = live-0.02)", "data", analyticsService.overallTotals());
        }
        if (q.contains("convert") && (q.contains("usd") || q.contains("gbp"))) {
            return Map.of("answer", "Adjusted GBP→USD conversion rate (live - 0.02)", "data", currencyService.getGbpToUsdAdjusted());
        }

        return Map.of("answer", "Sorry, I didn't understand. Try: 'country wise business', 'month wise totals', 'brand supplier links', or 'convert to USD'.");
    }
}

