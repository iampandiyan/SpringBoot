package com.example.trade.saga;

public enum SagaState {
    RUNNING,        // some steps done, more to do (or the process stopped and must resume)
    COMPLETED,      // every step succeeded
    COMPENSATED     // a step failed, the earlier ones were undone
}
