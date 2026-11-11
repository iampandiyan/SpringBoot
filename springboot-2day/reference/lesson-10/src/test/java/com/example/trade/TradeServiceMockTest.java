package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.trade.config.TradeProperties;
import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;
import com.example.trade.service.BusinessRuleException;
import com.example.trade.service.TradeNotFoundException;
import com.example.trade.service.TradeService;
import com.example.trade.service.TradeStateException;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// A pure unit test with a MOCK repository: no Spring, no database, runs in milliseconds.
// Unlike TradeServiceTest (which uses a working in-memory fake), a mock lets us check HOW the service talks to
// its collaborator: which calls it made, with what, and which calls it must NOT make.
@ExtendWith(MockitoExtension.class)        // creates the @Mock fields; fails the test on unused stubbings
class TradeServiceMockTest {

    @Mock TradeRepository repository;
    TradeService service;

    @BeforeEach
    void setUp() {
        service = new TradeService(repository, new TradeProperties(new BigDecimal("1000"), "TEST"),
                (type, aggregateId, payload) -> { });
    }

    @Test
    void createSavesANewTradeWithStatusNew() {
        given(repository.save(any(Trade.class))).willAnswer(inv -> inv.getArgument(0));

        service.create(TradeFixtures.ISIN, BigDecimal.TEN, BigDecimal.ONE);

        ArgumentCaptor<Trade> saved = ArgumentCaptor.forClass(Trade.class);
        verify(repository).save(saved.capture());            // exactly one save, and we can inspect its argument
        assertThat(saved.getValue().status()).isEqualTo(TradeStatus.NEW);
        assertThat(saved.getValue().id()).isNull();            // the database, not the service, assigns ids
    }

    @Test
    void aRejectedTradeNeverReachesTheRepository() {
        assertThatThrownBy(() -> service.create(TradeFixtures.ISIN, new BigDecimal("1001"), BigDecimal.ONE))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());                // proves the rule runs BEFORE any write
    }

    @Test
    void settleSavesTheTradeWithStatusSettled() {
        given(repository.findById(5L)).willReturn(Optional.of(TradeFixtures.savedTrade(5, TradeStatus.NEW)));
        given(repository.save(any(Trade.class))).willAnswer(inv -> inv.getArgument(0));

        Trade result = service.settle(5L);

        assertThat(result.status()).isEqualTo(TradeStatus.SETTLED);
    }

    @Test
    void settlingAnAlreadySettledTradeWritesNothing() {
        given(repository.findById(5L)).willReturn(Optional.of(TradeFixtures.savedTrade(5, TradeStatus.SETTLED)));

        assertThatThrownBy(() -> service.settle(5L)).isInstanceOf(TradeStateException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void settlingAnUnknownTradeIsNotFound() {
        given(repository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.settle(9L)).isInstanceOf(TradeNotFoundException.class);
    }
}
