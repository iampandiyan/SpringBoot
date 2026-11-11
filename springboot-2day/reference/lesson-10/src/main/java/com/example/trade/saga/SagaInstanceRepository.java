package com.example.trade.saga;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SagaInstanceRepository extends JpaRepository<SagaInstanceEntity, Long> {

    // What a recovery job asks at startup: "which sagas were interrupted?"
    List<SagaInstanceEntity> findByState(SagaState state);
}
