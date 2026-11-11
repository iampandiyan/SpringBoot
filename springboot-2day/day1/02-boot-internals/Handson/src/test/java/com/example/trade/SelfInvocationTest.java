package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import com.example.trade.service.TradeService;

@SpringBootTest(properties ="spring.main.web-application-type=none")
@ExtendWith(OutputCaptureExtension.class)
public class SelfInvocationTest {

    @Autowired TradeService service;

    @Test 
    void injectedBeanIsAProxy(){
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    @Test 
    void selfCallBypassesTheAspect(CapturedOutput output){
        var t=service.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        int mark=output.getOut().length();
        service.settleViaSelfCall(t.id());
        String after=output.getOut().substring(mark);
        assertThat(after).contains("TradeService.settleViaSelfCall(..) took");
        assertThat(after).doesNotContain("TradeService.settle(..) took"); 
    }
    
}
