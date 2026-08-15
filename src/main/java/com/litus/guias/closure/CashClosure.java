package com.litus.guias.closure;

import com.litus.guias.account.CashClosureItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CashClosure {
    private long id;

    private BigDecimal expectedCash;
    private BigDecimal countedCash;

    private BigDecimal expectedQr;
    private BigDecimal reportedQr;

    private String cancellationReason;
    private CashClosureStatus status;
    private String notes;
    private LocalDateTime createdAt;

    public CashClosure(
            long id,
            BigDecimal expectedCash,
            BigDecimal countedCash,
            BigDecimal expectedQr,
            BigDecimal reportedQr,
            String notes,
            LocalDateTime createdAt

    ) {
        this.id = id;
        this.expectedCash = expectedCash;
        this.countedCash = countedCash;
        this.expectedQr = expectedQr;
        this.reportedQr = reportedQr;
        this.notes = notes;
        this.createdAt = createdAt;
        this.status = CashClosureStatus.VALID;
    }

    public boolean isBalanced(List<CashClosureItem> items) {
        if (getCashDifference().compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }
        if (getQrDifference().compareTo(BigDecimal.ZERO) != 0) {
            return false;
        }

        for (CashClosureItem item : items) {
            if (item.getDifference() != 0) {
                return false;
            }
        }

        return true;
    }

    public void cancel(String reason) {
        if (status == CashClosureStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cash closure is already cancelled"
            );
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "Cancellation reason is required"
            );
        }

        status = CashClosureStatus.CANCELLED;
        cancellationReason = reason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public BigDecimal getCashDifference() {
        return countedCash.subtract(expectedCash);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getQrDifference() {
        return reportedQr.subtract(expectedQr);
    }

    public CashClosureStatus getStatus() {
        return status;
    }

}
