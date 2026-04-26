package com.invest.repo;

import com.invest.domain.DividendRecord;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DividendRecordRepository extends JpaRepository<DividendRecord, Long> {

    boolean existsByUserIdAndSymbolAndExDate(String userId, String symbol, LocalDate exDate);
}
