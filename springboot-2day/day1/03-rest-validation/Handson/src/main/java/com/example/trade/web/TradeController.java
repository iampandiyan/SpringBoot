package com.example.trade.web;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.service.TradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

// Web layer: translates HTTP <-> Java. It contains NO business logic; it only calls the service.
// @RestController = @Controller + @ResponseBody: return values are written as JSON.
@RestController
@RequestMapping("/trades")
public class TradeController {

    // Request body shape. Jackson turns JSON into this record, then Bean Validation checks the annotations
    // (because the parameter is marked @Valid). A record is perfect here: immutable and no boilerplate.
    public record CreateTradeRequest(
            @NotBlank @Pattern(regexp = "[A-Z]{2}[A-Z0-9]{9}[0-9]", message = "must be a 12-character ISIN") String isin,
            @NotNull @Positive BigDecimal quantity,
            @NotNull @Positive BigDecimal price) {}

    private final TradeService service;

    public TradeController(TradeService service) {
        this.service = service;
    }

    // 201 Created + Location header pointing at the new resource: the REST convention for POST that creates.
    @PostMapping
    public ResponseEntity<Trade> create(@Valid @RequestBody CreateTradeRequest req) {
        Trade created = service.create(req.isin(), req.quantity(), req.price());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    public Trade get(@PathVariable Long id) {
        return service.get(id);
    }

    // GET /trades?status=NEW. A bad value ("status=BOGUS") is rejected with 400 before this method runs.
    @GetMapping
    public List<Trade> list(@RequestParam(required = false) TradeStatus status) {
        return service.list(status);
    }

    // An action on a resource, modelled as POST on a sub-resource. Not idempotent here: a second call gives 409.
    @PostMapping("/{id}/settle")
    public Trade settle(@PathVariable Long id) {
        return service.settle(id);
    }
}