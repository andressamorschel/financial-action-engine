package com.financialactionengine.domain;

import com.financialactionengine.converter.CurrencyConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Currency;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "financial_actions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_action_client_idempotency_key",
                        columnNames = {"client_id", "idempotency_key"}
                )
        }
)
public class Action extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "client_id", nullable = false, length = 128)
    private String clientId;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(nullable = false, length = 255)
    private String description;

    @Column(nullable = false, length = 3)
    @Convert(converter = CurrencyConverter.class)
    private Currency currency;

    @Column(name = "risk_level")
    @Enumerated(EnumType.STRING)
    private ActionRisk riskLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionStatus status = ActionStatus.PROPOSED;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionSource source;

    @Column(nullable = false, name = "credit_account_id")
    private String creditAccountId;

    @Column(nullable = false, name = "debit_account_id")
    private String debitAccountId;
}