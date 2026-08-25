package com.litus.guias.ui;

import com.litus.guias.account.Account;
import com.litus.guias.closure.DayStatus;
import com.litus.guias.inventory.Guide;
import com.litus.guias.persistence.AccountRepository;
import com.litus.guias.persistence.Database;
import com.litus.guias.persistence.DayStatusQueryService;
import com.litus.guias.persistence.GuideRepository;
import com.litus.guias.persistence.SaleRepository;
import com.litus.guias.persistence.SupplierDebtQueryService;
import com.litus.guias.sale.Sale;
import com.litus.guias.sale.SaleStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppQueries {

    private final GuideRepository guides;
    private final AccountRepository accounts;
    private final SaleRepository sales;
    private final SupplierDebtQueryService debt;
    private final DayStatusQueryService dayStatus;

    public AppQueries(Database database) {
        guides = new GuideRepository(database);
        accounts = new AccountRepository(database);
        sales = new SaleRepository(database);
        debt = new SupplierDebtQueryService(database);
        dayStatus = new DayStatusQueryService(database);
    }

    public TodaySummary todaySummary(LocalDate day) throws Exception {
        List<Sale> todaySales = sales.findBetween(day.atStartOfDay(), day.plusDays(1).atStartOfDay());
        long active = todaySales.stream().filter(s -> s.getStatus() == SaleStatus.ACTIVE).count();
        BigDecimal income = todaySales.stream()
                .filter(s -> s.getStatus() == SaleStatus.ACTIVE)
                .map(Sale::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Account cash = accounts.findByName("Efectivo");
        Account qr = accounts.findByName("QR / Soto");
        return new TodaySummary(
                active,
                income,
                cash == null ? BigDecimal.ZERO : cash.getBalance(),
                qr == null ? BigDecimal.ZERO : qr.getBalance(),
                debt.calculateCurrentDebt(),
                dayStatus.getStatus(day, LocalDate.now())
        );
    }

    public Map<Long, String> guideNames() throws Exception {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Guide guide : guides.findAll()) result.put(guide.getId(), guide.getName());
        return result;
    }

    public Map<Long, String> accountNames() throws Exception {
        Map<Long, String> result = new LinkedHashMap<>();
        for (Account account : accounts.findAll()) result.put(account.getId(), account.getName());
        return result;
    }

    public List<GuideWeekSummary> weeklyGuideSummary(LocalDate referenceDay) throws Exception {
        LocalDate currentMonday = referenceDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate previousMonday = currentMonday.minusWeeks(1);
        List<Sale> fortnight = sales.findBetween(previousMonday.atStartOfDay(), currentMonday.plusWeeks(1).atStartOfDay());
        List<GuideWeekSummary> result = new ArrayList<>();
        for (Guide guide : guides.findAll()) {
            long current = fortnight.stream()
                    .filter(sale -> sale.getGuideId() == guide.getId())
                    .filter(sale -> sale.getStatus() == SaleStatus.ACTIVE)
                    .filter(sale -> !sale.getCreatedAt().toLocalDate().isBefore(currentMonday))
                    .count();
            long previous = fortnight.stream()
                    .filter(sale -> sale.getGuideId() == guide.getId())
                    .filter(sale -> sale.getStatus() == SaleStatus.ACTIVE)
                    .filter(sale -> sale.getCreatedAt().toLocalDate().isBefore(currentMonday))
                    .count();
            result.add(new GuideWeekSummary(guide.getId(), guide.getName(), current, previous));
        }
        return result;
    }

    public record TodaySummary(
            long activeSales,
            BigDecimal income,
            BigDecimal cashBalance,
            BigDecimal qrBalance,
            BigDecimal supplierDebt,
            DayStatus status
    ) {
    }

    public record GuideWeekSummary(long guideId, String guideName, long currentWeek, long previousWeek) {
    }
}
