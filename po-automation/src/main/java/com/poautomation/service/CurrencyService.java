package com.poautomation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class CurrencyService {

    private final RestTemplate restTemplate;

    @Value("${app.currency.rate-url}")
    private String rateUrl;

    private BigDecimal cachedRate = new BigDecimal("0.79");
    private LocalDateTime cacheTime;

    public CurrencyService(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    public Map<String, Object> getUsdToGbpRate() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(rateUrl, Map.class);
            Map<String, Object> body = response.getBody();
            if (body != null && body.get("rates") instanceof Map<?, ?> rates && rates.get("GBP") != null) {
                cachedRate = new BigDecimal(rates.get("GBP").toString());
                cacheTime = LocalDateTime.now();
                return Map.of(
                        "base", "USD",
                        "target", "GBP",
                        "rate", cachedRate,
                        "source", "LIVE_API",
                        "asOf", cacheTime.toString()
                );
            }
        } catch (Exception ignored) {
        }

        return Map.of(
                "base", "USD",
                "target", "GBP",
                "rate", cachedRate,
                "source", "FALLBACK_CACHE",
                "asOf", cacheTime == null ? "N/A" : cacheTime.toString()
        );
    }

    // Adjusted GBP->USD rate as per spec: live rate minus 0.02 (two cents)
    public BigDecimal getGbpToUsdAdjustedRate() {
        // If we have USD->GBP, invert to get GBP->USD
        BigDecimal usdToGbp = (BigDecimal) getUsdToGbpRate().get("rate");
        if (usdToGbp == null || usdToGbp.compareTo(BigDecimal.ZERO) == 0) {
            usdToGbp = new BigDecimal("0.78");
        }
        BigDecimal gbpToUsd = BigDecimal.ONE.divide(usdToGbp, 6, java.math.RoundingMode.HALF_UP);
        BigDecimal adjusted = gbpToUsd.subtract(new BigDecimal("0.02"));
        return adjusted.max(BigDecimal.ZERO);
    }

    public Map<String, Object> getGbpToUsdAdjusted() {
        BigDecimal rate = getGbpToUsdAdjustedRate();
        return Map.of("base", "GBP", "target", "USD", "rate", rate);
    }

    public BigDecimal getUsdToGbpAdjustedRate() {
        BigDecimal gbpToUsd = getGbpToUsdAdjustedRate();
        if (gbpToUsd == null || gbpToUsd.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("0.78");
        }
        // USD->GBP adjusted is the inverse of GBP->USD adjusted.
        return BigDecimal.ONE.divide(gbpToUsd, 6, java.math.RoundingMode.HALF_UP);
    }
}
