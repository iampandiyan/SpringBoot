package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.trade.domain.TradeStatus;
import com.example.trade.service.TradeNotFoundException;
import com.example.trade.service.TradeService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

// Deliberately NOT @Transactional: if the test method opened its own transaction, the service's rollback
// rules would be hidden. Each service call here is its own transaction, so we see real commit/rollback.
@SpringBootTest(properties = "spring.main.web-application-type=none")
class TransactionTest {

    @Autowired TradeService service;

    private Long newTrade() {
        return service.create("US0378331005", BigDecimal.TEN, BigDecimal.ONE).id();
    }

    @Test
    void settleAllCommitsEverythingWhenAllSucceed() {
        Long a = newTrade(), b = newTrade();
        service.settleAll(List.of(a, b));
        assertThat(service.get(a).status()).isEqualTo(TradeStatus.SETTLED);
        assertThat(service.get(b).status()).isEqualTo(TradeStatus.SETTLED);
    }

    @Test
    void uncheckedExceptionRollsBackTheWholeBatch() {
        Long a = newTrade();

        assertThatThrownBy(() -> service.settleAll(List.of(a, 999_999L)))
                .isInstanceOf(TradeNotFoundException.class);

        // Trade "a" was settled inside the transaction, but the rollback undid it.
        assertThat(service.get(a).status()).isEqualTo(TradeStatus.NEW);
    }

    @Test
    void checkedExceptionDoesNotRollBackByDefault() {
        Long a = newTrade();

        assertThatThrownBy(() -> service.settleThenFailChecked(a)).hasMessage("checked failure after settle");

        assertThat(service.get(a).status()).isEqualTo(TradeStatus.SETTLED);   // committed despite the exception!
    }

    @Test
    void rollbackForMakesACheckedExceptionRollBack() {
        Long a = newTrade();

        assertThatThrownBy(() -> service.settleThenFailCheckedWithRollbackFor(a)).hasMessage("checked failure after settle");

        assertThat(service.get(a).status()).isEqualTo(TradeStatus.NEW);
    }
}
