package org.bhargavguntupalli.tradingsandboxapi.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "holdings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "symbol"}))
public class Holding {

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
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal quantity;

    @Getter @Setter
    @Column(name = "average_cost", nullable = false, precision = 19, scale = 4)
    private BigDecimal averageCost;
}
