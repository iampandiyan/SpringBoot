package com.example.trade.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

// A row of the READ MODEL. Its id is the trade id (assigned by us, not generated): saving the same trade twice
// updates the same row, which is exactly what makes the projector idempotent.
@Entity
@Table(name = "settled_trade_report")
public class SettledTradeReportEntity {

    @Id
    @Column(name = "trade_id")
    private Long tradeId;

    @Column(nullable = false, length = 12)
    private String isin;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal notional;

    @Column(name = "settled_at", nullable = false)
    private Instant settledAt;

    protected SettledTradeReportEntity() {}

    public SettledTradeReportEntity(Long tradeId, String isin, BigDecimal quantity, BigDecimal notional, Instant settledAt) {
        this.tradeId = tradeId;
        this.isin = isin;
        this.quantity = quantity;
        this.notional = notional;
        this.settledAt = settledAt;
    }

    public Long getTradeId() { return tradeId; }

    public String getIsin() { return isin; }

    public BigDecimal getQuantity() { return quantity; }

    public BigDecimal getNotional() { return notional; }

    public Instant getSettledAt() { return settledAt; }
}
