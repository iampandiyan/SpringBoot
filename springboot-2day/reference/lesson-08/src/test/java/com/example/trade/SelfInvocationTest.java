package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.service.TradeService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;

// Needs the Spring context (unlike TradeServiceTest) because we are testing the PROXY itself.
@SpringBootTest(properties = "spring.main.web-application-type=none")
@ExtendWith(OutputCaptureExtension.class)
class SelfInvocationTest {

    @Autowired TradeService service;

    @Test
    void injectedBeanIsAProxy() {
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    @Test
    void selfCallBypassesTheAspect(CapturedOutput output) {
        var t = service.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE);
        int mark = output.getOut().length();            // ignore everything logged so far

        service.settleViaSelfCall(t.id());

        String after = output.getOut().substring(mark);
        assertThat(after).contains("TradeService.settleViaSelfCall(..) took"); // outer call: through proxy
        assertThat(after).doesNotContain("TradeService.settle(..) took");      // inner call: bypassed
    }
}
