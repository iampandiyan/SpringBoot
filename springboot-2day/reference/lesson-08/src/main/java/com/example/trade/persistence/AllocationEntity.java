package com.example.trade.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

// One trade is split across several client accounts: many allocations belong to one trade.
@Entity
@Table(name = "allocation")
public class AllocationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The owning side of the relation: this table holds the foreign key column TRADE_ID.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)    // @ManyToOne is EAGER by default: always set LAZY
    @JoinColumn(name = "trade_id")
    private TradeEntity trade;

    @Column(nullable = false, length = 20)
    private String account;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal quantity;

    protected AllocationEntity() {}

    AllocationEntity(TradeEntity trade, String account, BigDecimal quantity) {
        this.trade = trade;
        this.account = account;
        this.quantity = quantity;
    }

    public String getAccount() { return account; }

    public BigDecimal getQuantity() { return quantity; }
}
