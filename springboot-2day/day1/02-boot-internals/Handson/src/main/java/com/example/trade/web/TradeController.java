package com.example.trade.web;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.trade.domain.Trade;
import com.example.trade.service.TradeService;

@RestController 
@RequestMapping ("/trades")
public class TradeController {
    public record CreateTradeRequest(String isin, BigDecimal quantity, BigDecimal price){}
    private final TradeService service;
    public TradeController(TradeService service){
        this.service=service;
    }

    @PostMapping 
    public Trade create(@RequestBody CreateTradeRequest request){
        return service.create(request.isin, request.quantity, request.price);
    }

    @PostMapping ("/{id}/settle")
    public Trade settle(@PathVariable Long id){
        return service.settle(id);
    }
}
