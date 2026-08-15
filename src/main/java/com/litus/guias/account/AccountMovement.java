package com.litus.guias.account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AccountMovement {

    private long id;
    private long accountId;
    private AccountMovementType type;
    private AccountMovementConcept concept;
    private BigDecimal amount;
    private String reason;
    private LocalDateTime createdAt;

    public AccountMovement(
            long id,
            long accountId,
            AccountMovementType type,
            AccountMovementConcept concept,
            BigDecimal amount,
            String reason,
            LocalDateTime createdAt
    ) {
        if (type == AccountMovementType.EXPENSE &&
            (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException(
                    "Expense reason is required"
            );
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        this.id = id;
        this.accountId = accountId;
        this.type = type;
        this.concept = concept;
        this.amount = amount;
        this.reason = reason;
        this.createdAt = createdAt;
    }

    public BigDecimal getAmount() {
        return this.amount;
    }

    public AccountMovementConcept getConcept() {
        return this.concept;
    }

    public String getReason() {
        return this.reason;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public AccountMovementType getType() {
        return this.type;
    }

    public long getAccountId() {
        return this.accountId;
    }
}
