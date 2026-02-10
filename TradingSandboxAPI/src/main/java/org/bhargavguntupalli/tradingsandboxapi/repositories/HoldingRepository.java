package org.bhargavguntupalli.tradingsandboxapi.repositories;

import org.bhargavguntupalli.tradingsandboxapi.models.Holding;
import org.bhargavguntupalli.tradingsandboxapi.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUser(User user);
    Optional<Holding> findByUserAndSymbol(User user, String symbol);
}
