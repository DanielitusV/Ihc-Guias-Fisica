package com.litus.guias.ui;

import com.litus.guias.persistence.AccountMovementRepository;
import com.litus.guias.persistence.AccountRepository;
import com.litus.guias.persistence.CashClosureRepository;
import com.litus.guias.persistence.CashClosureTransactionService;
import com.litus.guias.persistence.Database;
import com.litus.guias.persistence.DayStatusQueryService;
import com.litus.guias.persistence.ExpenseTransactionService;
import com.litus.guias.persistence.GuideRepository;
import com.litus.guias.persistence.OrderRepository;
import com.litus.guias.persistence.OrderTransactionService;
import com.litus.guias.persistence.SaleRepository;
import com.litus.guias.persistence.SaleTransactionService;
import com.litus.guias.persistence.SupplierDebtQueryService;
import com.litus.guias.persistence.SupplierPaymentTransactionService;
import com.litus.guias.persistence.AcademicTermRepository;
import com.litus.guias.persistence.AcademicTermService;
import com.litus.guias.persistence.ManualIncomeTransactionService;
import com.litus.guias.persistence.ClosureSnapshotQueryService;
import com.litus.guias.persistence.AuthorizedDeliveryRepository;
import com.litus.guias.persistence.AuthorizedDeliveryTransactionService;
import com.litus.guias.persistence.AuthorizedDeliveryReturnRepository;
import com.litus.guias.persistence.AuthorizedDeliveryReturnTransactionService;
import com.litus.guias.persistence.MoneyTransferTransactionService;
import com.litus.guias.persistence.InventoryAdjustmentRepository;
import com.litus.guias.persistence.InventoryAdjustmentTransactionService;

import java.util.ArrayList;
import java.util.List;

public final class AppContext {
    public final Database database;
    public final GuideRepository guides;
    public final AccountRepository accounts;
    public final SaleRepository sales;
    public final OrderRepository orders;
    public final AccountMovementRepository movements;
    public final CashClosureRepository closures;
    public final SaleTransactionService saleTransactions;
    public final OrderTransactionService orderTransactions;
    public final ExpenseTransactionService expenseTransactions;
    public final SupplierPaymentTransactionService supplierPayments;
    public final CashClosureTransactionService closureTransactions;
    public final SupplierDebtQueryService supplierDebt;
    public final DayStatusQueryService dayStatus;
    public final AppQueries queries;
    public final AcademicTermRepository terms;
    public final AcademicTermService termService;
    public final ManualIncomeTransactionService manualIncome;
    public final ClosureSnapshotQueryService closureSnapshots;
    public final AuthorizedDeliveryRepository authorizedDeliveries;
    public final AuthorizedDeliveryTransactionService authorizedDeliveryTransactions;
    public final AuthorizedDeliveryReturnRepository authorizedDeliveryReturns;
    public final AuthorizedDeliveryReturnTransactionService authorizedDeliveryReturnTransactions;
    public final MoneyTransferTransactionService moneyTransfers;
    public final InventoryAdjustmentRepository inventoryAdjustments;
    public final InventoryAdjustmentTransactionService inventoryAdjustmentTransactions;

    private final List<Runnable> refreshListeners = new ArrayList<>();

    public AppContext(Database database) {
        this.database = database;
        guides = new GuideRepository(database);
        accounts = new AccountRepository(database);
        sales = new SaleRepository(database);
        orders = new OrderRepository(database);
        movements = new AccountMovementRepository(database);
        closures = new CashClosureRepository(database);
        saleTransactions = new SaleTransactionService(database);
        orderTransactions = new OrderTransactionService(database);
        expenseTransactions = new ExpenseTransactionService(database);
        supplierPayments = new SupplierPaymentTransactionService(database);
        closureTransactions = new CashClosureTransactionService(database);
        supplierDebt = new SupplierDebtQueryService(database);
        dayStatus = new DayStatusQueryService(database);
        queries = new AppQueries(database);
        terms = new AcademicTermRepository(database);
        termService = new AcademicTermService(database);
        manualIncome = new ManualIncomeTransactionService(database);
        closureSnapshots = new ClosureSnapshotQueryService(database);
        authorizedDeliveries = new AuthorizedDeliveryRepository(database);
        authorizedDeliveryTransactions = new AuthorizedDeliveryTransactionService(database);
        authorizedDeliveryReturns = new AuthorizedDeliveryReturnRepository(database);
        authorizedDeliveryReturnTransactions = new AuthorizedDeliveryReturnTransactionService(database);
        moneyTransfers = new MoneyTransferTransactionService(database);
        inventoryAdjustments = new InventoryAdjustmentRepository(database);
        inventoryAdjustmentTransactions = new InventoryAdjustmentTransactionService(database);
    }

    public void onRefresh(Runnable listener) {
        refreshListeners.add(listener);
    }

    public void refreshAll() {
        for (Runnable listener : List.copyOf(refreshListeners)) {
            listener.run();
        }
    }
}
