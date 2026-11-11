package com.example.trade.web;

import com.example.trade.gateway.SettlementConfirmation;
import com.example.trade.service.ConfirmationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trades")
public class ConfirmationController {

    private final ConfirmationService confirmations;

    public ConfirmationController(ConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    // 200 OK: the gateway confirmed. 202 Accepted: we could not reach it, the request is queued and will be completed later.
    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasRole('OPS')")
    public ResponseEntity<SettlementConfirmation> confirm(@PathVariable Long id) {
        SettlementConfirmation result = confirmations.confirm(id);
        return result.confirmed() ? ResponseEntity.ok(result) : ResponseEntity.accepted().body(result);
    }
}
