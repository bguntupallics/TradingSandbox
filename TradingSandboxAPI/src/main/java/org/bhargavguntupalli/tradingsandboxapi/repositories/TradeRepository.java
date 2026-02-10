package org.bhargavguntupalli.tradingsandboxapi.repositories;

import org.bhargavguntupalli.tradingsandboxapi.models.Trade;
import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByUserOrderByExecutedAtDesc(User user);
}
