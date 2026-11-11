package com.example.trade.report;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettledTradeReportRepository extends JpaRepository<SettledTradeReportEntity, Long> {

    List<SettledTradeReportEntity> findByIsinOrderBySettledAtDesc(String isin);
}
