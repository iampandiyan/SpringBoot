package com.example.trade.persistence;

import com.example.trade.domain.Trade;
import com.example.trade.domain.TradeStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// The JPA entity: a mutable class mapped to the TRADE table. It stays inside the persistence package.
// The rest of the app works with the immutable Trade record, so JPA details (lazy collections, proxies,
// no-arg constructors) never leak into the service or the JSON.
@Entity
@Table(name = "trade")
public class TradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // the database assigns the id
    private Long id;

    @Column(nullable = false, length = 12)
    private String isin;

    @Column(nullable = false, precision = 19, scale = 0)    // whole units
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)                            // never ORDINAL: reordering the enum would corrupt data
    @Column(nullable = false, length = 10)
    private TradeStatus status;

    // LAZY is the default for collections: the allocations are NOT loaded until someone touches the list.
    // That default is the root of the N+1 problem demonstrated in NPlusOneTest.
    @OneToMany(mappedBy = "trade", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AllocationEntity> allocations = new ArrayList<>();

    protected TradeEntity() {}                              // JPA needs a no-arg constructor

    public TradeEntity(String isin, BigDecimal quantity, BigDecimal price, TradeStatus status) {
        this.isin = isin;
        this.quantity = quantity;
        this.price = price;
        this.status = status;
    }

    public void addAllocation(String account, BigDecimal qty) {
        allocations.add(new AllocationEntity(this, account, qty));   // keep both sides of the relation in sync
    }

    // Copy the editable fields from the domain record onto this entity.
    void apply(Trade t) {
        this.isin = t.isin();
        this.quantity = t.quantity();
        this.price = t.price();
        this.status = t.status();
    }

    Trade toDomain() {
        return new Trade(id, isin, quantity, price, status);
    }

    public Long getId() { return id; }

    public List<AllocationEntity> getAllocations() { return allocations; }
}
