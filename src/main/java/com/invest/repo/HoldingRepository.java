package com.invest.repo;

import com.invest.domain.Holding;
import com.invest.domain.Holding.HoldingId;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HoldingRepository extends JpaRepository<Holding, HoldingId> {

    List<Holding> findByUserId(String userId);

    Optional<Holding> findByUserIdAndSymbol(String userId, String symbol);

    @Query("select distinct upper(h.symbol) from Holding h where h.quantity > 0")
    List<String> findDistinctActiveSymbols();
}
