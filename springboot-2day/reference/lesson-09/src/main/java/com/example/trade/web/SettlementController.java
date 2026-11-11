package com.example.trade.web;

import com.example.trade.saga.SagaState;
import com.example.trade.saga.SettlementOutcome;
import com.example.trade.saga.SettlementSaga;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
public class SettlementController {

    private final SettlementSaga saga;

    public SettlementController(SettlementSaga saga) {
        this.saga = saga;
    }

    // 200: the saga completed. 422: it ran, a step failed, and everything was rolled back (business outcome, not a crash).
    @PostMapping("/{id}/settlement")
    @PreAuthorize("hasRole('OPS')")
    public ResponseEntity<SettlementOutcome> settle(@PathVariable Long id) {
        SettlementOutcome outcome = saga.run(id);
        return outcome.state() == SagaState.COMPLETED
                ? ResponseEntity.ok(outcome)
                : ResponseEntity.unprocessableEntity().body(outcome);
    }
}
