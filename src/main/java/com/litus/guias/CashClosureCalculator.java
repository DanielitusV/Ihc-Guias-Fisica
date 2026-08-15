package com.litus.guias;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CashClosureCalculator {

    public int calculateSoldQuantity(
            int previousStock,
            int receivedQuantity,
            int countedStock
    ) {
        return previousStock + receivedQuantity - countedStock;
    }

    public int calculateSalesDifference(
            int soldByInventory,
            int registeredSales
    ) {
        return registeredSales - soldByInventory;
    }

    public BigDecimal calculatedExpectedMoney(
            int soldQuantity,
            BigDecimal price
    ) {
        return price.multiply(
                BigDecimal.valueOf(soldQuantity)
        );
    }

    public BigDecimal calculatedTotalExpectedMoney(
            List<BigDecimal> amounts
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (BigDecimal amount : amounts) {
            total = total.add(amount);
        }

        return total;
    }

    public BigDecimal calculateMoneyDifference(
            BigDecimal expectedMoney,
            BigDecimal registeredMoney
    ) {
        return registeredMoney.subtract(expectedMoney);
    }

    public BigDecimal calculateRegisteredSalesTotal(
            List<Sale> sales,
            PaymentMethod paymentMethod
    ) {
        BigDecimal total = BigDecimal.ZERO;

        for (Sale sale : sales) {
            if (
                    sale.getPaymentMethod() == paymentMethod
                    && sale.getStatus() == SaleStatus.ACTIVE
            ) {
                total = total.add(sale.getPrice());
            }
        }
        return total;
    }

    public BigDecimal calculateRegisteredSalesTotal(
            List<Sale> sales,
            PaymentMethod paymentMethod,
            LocalDateTime clousureTime
    ) {
         BigDecimal total = BigDecimal.ZERO;

         for (Sale sale: sales) {
             boolean sameDay =
                     sale.getCreatedAt().toLocalDate()
                             .equals(clousureTime.toLocalDate());

             boolean beforeOrAtClosure =
                     !sale.getCreatedAt().isAfter(clousureTime);

             if (
                     sale.getPaymentMethod() == paymentMethod
                     && sale.getStatus() == SaleStatus.ACTIVE
                     && sameDay
                     && beforeOrAtClosure
             ) {
                 total = total.add(sale.getPrice());
             }
         }

         return total;
    }
}
