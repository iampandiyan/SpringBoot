package com.example.trade.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// Spring Data writes the implementation at startup: save, findById, findAll, count, delete... for free.
// We only declare the extra queries we need.
public interface TradeJpaRepository extends JpaRepository<TradeEntity, Long> {

    // JOIN FETCH loads trades AND their allocations in ONE SQL statement. "distinct" removes the duplicate
    // trade rows that the join produces (one per allocation).
    @Query("select distinct t from TradeEntity t left join fetch t.allocations")
    List<TradeEntity> findAllWithAllocations();
}
