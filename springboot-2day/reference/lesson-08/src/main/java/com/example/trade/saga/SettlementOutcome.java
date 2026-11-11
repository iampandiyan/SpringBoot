package com.example.trade.saga;

public record SettlementOutcome(Long sagaId, Long tradeId, SagaState state, String failureReason) {}
