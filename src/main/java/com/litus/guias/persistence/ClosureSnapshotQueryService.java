package com.litus.guias.persistence;

import com.litus.guias.account.Account;
import com.litus.guias.account.AccountMovement;
import com.litus.guias.account.AccountMovementType;
import com.litus.guias.inventory.Guide;
import com.litus.guias.order.Order;
import com.litus.guias.order.OrderItem;
import com.litus.guias.order.OrderStatus;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClosureSnapshotQueryService {
    private static final LocalDateTime BEGINNING = LocalDateTime.of(1, 1, 1, 0, 0);

    private final GuideRepository guides;
    private final AccountRepository accounts;
    private final AccountMovementRepository movements;
    private final OrderRepository orders;
    private final SaleRepository sales;
    private final AuthorizedDeliveryRepository deliveries;
    private final AuthorizedDeliveryReturnRepository deliveryReturns;
    private final InventoryAdjustmentRepository adjustments;

    public ClosureSnapshotQueryService(Database database) {
        guides = new GuideRepository(database);
        accounts = new AccountRepository(database);
        movements = new AccountMovementRepository(database);
        orders = new OrderRepository(database);
        sales = new SaleRepository(database);
        deliveries = new AuthorizedDeliveryRepository(database);
        deliveryReturns = new AuthorizedDeliveryReturnRepository(database);
        adjustments = new InventoryAdjustmentRepository(database);
    }

    public Snapshot atEndOf(LocalDate day) throws Exception {
        LocalDateTime cutoff = day.plusDays(1).atStartOfDay();
        Account cash = accounts.findByName("Efectivo");
        Account qr = accounts.findByName("QR / Soto");
        Map<Long, BigDecimal> balances = new LinkedHashMap<>();
        for (AccountMovement movement : movements.findBetween(BEGINNING, cutoff)) {
            BigDecimal signed = movement.getType() == AccountMovementType.INCOME
                    ? movement.getAmount() : movement.getAmount().negate();
            balances.merge(movement.getAccountId(), signed, BigDecimal::add);
        }

        Map<Long, Integer> stock = new LinkedHashMap<>();
        for (Guide guide : guides.findAll()) stock.put(guide.getId(), 0);
        for (var adjustment : adjustments.findBetween(BEGINNING, cutoff)) {
            stock.merge(adjustment.guideId(), adjustment.quantityDelta(), Integer::sum);
        }
        for (Order order : orders.findAll()) {
            if (!order.getCreatedAt().isBefore(cutoff)) continue;
            boolean activeAtCutoff = order.getStatus() == OrderStatus.ACTIVE
                    || (order.getCancelledAt() != null && !order.getCancelledAt().isBefore(cutoff));
            if (!activeAtCutoff) continue;
            for (OrderItem item : order.getItems()) {
                stock.merge(item.getGuideId(), item.getQuantity(), Integer::sum);
            }
        }
        List<Sale> historicSales = sales.findBetween(BEGINNING, cutoff);
        for (Sale sale : historicSales) {
            boolean activeAtCutoff = sale.getStatus() == SaleStatus.ACTIVE
                    || (sale.getCancelledAt() != null && !sale.getCancelledAt().isBefore(cutoff));
            if (activeAtCutoff) stock.merge(sale.getGuideId(), -1, Integer::sum);
        }
        for (var delivery : deliveries.findBetween(BEGINNING, cutoff)) {
            stock.merge(delivery.guideId(), -delivery.quantity(), Integer::sum);
        }
        for (var deliveryReturn : deliveryReturns.findBetween(BEGINNING, cutoff)) {
            stock.merge(deliveryReturn.guideId(), deliveryReturn.quantity(), Integer::sum);
        }

        return new Snapshot(
                cash == null ? BigDecimal.ZERO : balances.getOrDefault(cash.getId(), BigDecimal.ZERO),
                qr == null ? BigDecimal.ZERO : balances.getOrDefault(qr.getId(), BigDecimal.ZERO),
                Map.copyOf(stock)
        );
    }

    public record Snapshot(
            BigDecimal expectedCash,
            BigDecimal expectedQr,
            Map<Long, Integer> expectedStock
    ) {
    }
}
