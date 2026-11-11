package com.example.trade.saga;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// The saga's memory, saved after every step. If the process dies, this row says where to continue.
@Entity
@Table(name = "saga_instance")
public class SagaInstanceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trade_id", nullable = false)
    private Long tradeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SagaState state;

    @Column(name = "next_step", nullable = false)
    private int nextStep;                        // index of the next step to run

    @Column(name = "failure_reason", length = 300)
    private String failureReason;

    protected SagaInstanceEntity() {}

    public SagaInstanceEntity(Long tradeId) {
        this.tradeId = tradeId;
        this.state = SagaState.RUNNING;
        this.nextStep = 0;
    }

    public Long getId() { return id; }

    public Long getTradeId() { return tradeId; }

    public SagaState getState() { return state; }

    public int getNextStep() { return nextStep; }

    public String getFailureReason() { return failureReason; }

    void stepDone() { nextStep++; }

    void completed() { state = SagaState.COMPLETED; }

    void compensated(String reason) {
        state = SagaState.COMPENSATED;
        failureReason = reason.length() > 300 ? reason.substring(0, 300) : reason;
    }
}
