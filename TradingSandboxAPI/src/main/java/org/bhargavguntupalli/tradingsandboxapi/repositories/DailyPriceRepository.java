package org.bhargavguntupalli.tradingsandboxapi.repositories;

import org.bhargavguntupalli.tradingsandboxapi.models.DailyPrice;
import org.bhargavguntupalli.tradingsandboxapi.models.DailyPriceId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyPriceRepository extends JpaRepository<DailyPrice, DailyPriceId> {
    // find one
    Optional<DailyPrice> findById(DailyPriceId id);

    // find all prices for a symbol, ordered by date
    List<DailyPrice> findByIdSymbolOrderByIdDateAsc(String symbol);

    // find all prices for a symbol between two dates (inclusive), ordered by date
    List<DailyPrice> findByIdSymbolAndIdDateBetweenOrderByIdDateAsc(String symbol, LocalDate start, LocalDate end);
}
