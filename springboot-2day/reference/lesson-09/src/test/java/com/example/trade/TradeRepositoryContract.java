package com.example.trade;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import com.example.trade.repo.TradeRepository;
import org.junit.jupiter.api.Test;

// A CONTRACT test: it states what ANY TradeRepository must do, and each implementation runs the same tests
// by extending this class. It is how we prove that swapping in-memory for JPA (lesson 04) kept the behaviour.
abstract class TradeRepositoryContract {

    /** Each implementation's test class supplies the repository under test. */
    abstract TradeRepository repository();

    @Test
    void saveAssignsAnId() {
        Trade saved = repository().save(TradeFixtures.newTrade());

        assertThat(saved.id()).isNotNull();
    }

    @Test
    void findByIdReturnsWhatWasSaved() {
        Trade saved = repository().save(TradeFixtures.newTrade());

        assertThat(repository().findById(saved.id())).hasValueSatisfying(found -> {
            assertThat(found.isin()).isEqualTo(TradeFixtures.ISIN);
            assertThat(found.quantity()).isEqualByComparingTo("100");      // by value: scale may differ per store
            assertThat(found.price()).isEqualByComparingTo("189.50");
            assertThat(found.status()).isEqualTo(TradeStatus.NEW);
        });
    }

    @Test
    void findByIdOfAnUnknownIdIsEmpty() {
        assertThat(repository().findById(987_654L)).isEmpty();
    }

    @Test
    void savingWithAnExistingIdUpdatesInsteadOfInserting() {
        Trade saved = repository().save(TradeFixtures.newTrade());
        int before = repository().findAll().size();

        repository().save(saved.withStatus(TradeStatus.SETTLED));

        assertThat(repository().findById(saved.id()).orElseThrow().status()).isEqualTo(TradeStatus.SETTLED);
        assertThat(repository().findAll()).hasSize(before);
    }

    @Test
    void findAllReturnsEverythingSaved() {
        int before = repository().findAll().size();

        repository().save(TradeFixtures.newTrade());
        repository().save(TradeFixtures.newTrade());

        assertThat(repository().findAll()).hasSize(before + 2);
    }
}
