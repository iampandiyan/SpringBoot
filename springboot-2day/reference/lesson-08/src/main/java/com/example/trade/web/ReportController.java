package com.example.trade.web;

import com.example.trade.report.SettledTradeReportEntity;
import com.example.trade.report.SettledTradeReportRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// The QUERY side. It reads only the read model, never the trade table, and it can never change anything.
// The data is EVENTUALLY consistent: a settled trade appears here once the relay has published its event.
@RestController
@RequestMapping("/reports")
public class ReportController {

    public record SettledTradeRow(Long tradeId, String isin, BigDecimal quantity, BigDecimal notional, Instant settledAt) {}

    private final SettledTradeReportRepository reports;

    public ReportController(SettledTradeReportRepository reports) {
        this.reports = reports;
    }

    @GetMapping("/settled")
    public List<SettledTradeRow> settled(@RequestParam String isin) {
        return reports.findByIsinOrderBySettledAtDesc(isin).stream()
                .map(r -> new SettledTradeRow(r.getTradeId(), r.getIsin(), r.getQuantity(), r.getNotional(), r.getSettledAt()))
                .toList();
    }
}
