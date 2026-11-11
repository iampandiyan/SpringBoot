package com.example.trade.service;

import com.example.trade.domain.Trade;
import com.example.trade.gateway.ResilientGatewayClient;
import com.example.trade.gateway.SettlementConfirmation;
import org.springframework.stereotype.Service;

// NOT @Transactional on purpose: trades.get(...) opens and COMMITS its own short transaction. We must never hold a
// database transaction (and its pooled connection) open while waiting for a slow remote system.
@Service
public class ConfirmationService {

    private final TradeService trades;
    private final ResilientGatewayClient gateway;

    public ConfirmationService(TradeService trades, ResilientGatewayClient gateway) {
        this.trades = trades;
        this.gateway = gateway;
    }

    public SettlementConfirmation confirm(Long tradeId) {
        Trade trade = trades.get(tradeId);          // 404 if unknown
        return gateway.confirm(trade);              // goes through the resilience proxy
    }
}
