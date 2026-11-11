package com.example.trade.health;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.stereotype.Component;

@Component 
public class GatewayStatus {
    private final AtomicBoolean up=new AtomicBoolean();
    public boolean isUp(){
        return up.get();
    }
    public void setUp(boolean up){
        this.up.set(up);
    }
}
