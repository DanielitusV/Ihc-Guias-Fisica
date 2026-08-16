package com.litus.guias.sale;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Sale {

    private long id;
    private long guideId;
    private long accountId;
    private BigDecimal price;
    private PaymentMethod paymentMethod;
    private LocalDateTime createdAt;
    private SaleStatus status;
    private String cancellationReason;

    public Sale(long id, long guideId, BigDecimal price,
                PaymentMethod paymentMethod, LocalDateTime createdAt,
                SaleStatus status) {
        this(id, guideId, 0, price, paymentMethod, createdAt, status);
    }

    public Sale(long id, long guideId, long accountId, BigDecimal price,
                PaymentMethod paymentMethod, LocalDateTime createdAt,
                SaleStatus status) {
        this.id = id;
        this.guideId = guideId;
        this.accountId = accountId;
        this.price = price;
        this.paymentMethod = paymentMethod;
        this.createdAt = createdAt;
        this.status = status;
    }

    public void cancel(String reason) {
        if (status == SaleStatus.CANCELLED) {
            throw new IllegalStateException("Sale is already cancelled");
        }

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }

        this.status = SaleStatus.CANCELLED;
        this.cancellationReason = reason;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public PaymentMethod getPaymentMethod() {
        return this.paymentMethod;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public long getId() {
        return this.id;
    }

    public long getGuideId() {
        return this.guideId;
    }

    public long getAccountId() {
        return accountId;
    }

    public SaleStatus getStatus() {
        return this.status;
    }

    public String getCancellationReason() {
        return this.cancellationReason;
    }
}
