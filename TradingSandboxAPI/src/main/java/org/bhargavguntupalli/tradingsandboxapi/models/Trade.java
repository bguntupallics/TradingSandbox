package org.bhargavguntupalli.tradingsandboxapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades", indexes = {
    @Index(name = "idx_trades_user_date", columnList = "user_id, executed_at DESC")
})
public class Trade {

    @Getter
    @Id @GeneratedValue
    private Long id;

    @Getter @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Getter @Setter
    @Column(nullable = false, length = 10)
    private String symbol;

    @Getter @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private TradeType type;

    @Getter @Setter
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity;

    @Getter @Setter
    @Column(name = "price_per_share", nullable = false, precision = 19, scale = 4)
    private BigDecimal pricePerShare;

    @Getter @Setter
    @Column(name = "total_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalCost;

    @Getter @Setter
    @Column(name = "executed_at", nullable = false)
    private LocalDateTime executedAt;
}
