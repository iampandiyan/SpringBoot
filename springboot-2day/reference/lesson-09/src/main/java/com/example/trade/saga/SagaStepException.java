package com.example.trade.saga;

// A step could not do its job. The orchestrator answers by compensating the earlier steps.
public class SagaStepException extends RuntimeException {
    public SagaStepException(String message) {
        super(message);
    }
}
