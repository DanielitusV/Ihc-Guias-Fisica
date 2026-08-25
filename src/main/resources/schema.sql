CREATE TABLE IF NOT EXISTS academic_terms (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    code TEXT NOT NULL UNIQUE CHECK (code GLOB '[12]-[0-9][0-9][0-9][0-9]'),
    status TEXT NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_at TEXT NOT NULL,
    closed_at TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_academic_terms_single_open
ON academic_terms(status) WHERE status = 'OPEN';

CREATE TABLE IF NOT EXISTS guides (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    current_price NUMERIC NOT NULL CHECK (current_price > 0),
    default_unit_cost NUMERIC NOT NULL DEFAULT 0 CHECK (default_unit_cost >= 0),
    stock INTEGER NOT NULL CHECK (stock >= 0)
);

CREATE TABLE IF NOT EXISTS accounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    balance NUMERIC NOT NULL
);

CREATE TABLE IF NOT EXISTS sales (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guide_id INTEGER NOT NULL,
    account_id INTEGER NOT NULL,
    price NUMERIC NOT NULL CHECK (price > 0),
    payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'QR')),
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
    cancellation_reason TEXT,
    cancelled_at TEXT,
    created_at TEXT NOT NULL,
    academic_term_id INTEGER,

    FOREIGN KEY (guide_id) REFERENCES guides(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
);

CREATE TABLE IF NOT EXISTS account_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    concept TEXT NOT NULL CHECK (concept IN ('SALE', 'GENERAL_EXPENSE', 'SUPPLIER_PAYMENT', 'SALE_CANCELLATION', 'CLOSURE_ADJUSTMENT', 'TRANSFER', 'OTHER')),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    reason TEXT,
    created_at TEXT NOT NULL,
    academic_term_id INTEGER,

    FOREIGN KEY (account_id) REFERENCES accounts(id),
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_condition TEXT NOT NULL CHECK (payment_condition IN ('PAID', 'CREDIT')),
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELLED', 'CORRECTED')),
    cancellation_reason TEXT,
    cancelled_at TEXT,
    replacement_order_id INTEGER,
    created_at TEXT NOT NULL,
    academic_term_id INTEGER,
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id),
    FOREIGN KEY (replacement_order_id) REFERENCES orders(id)
);

CREATE TABLE IF NOT EXISTS inventory_adjustments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guide_id INTEGER NOT NULL,
    quantity_delta INTEGER NOT NULL CHECK (quantity_delta <> 0),
    adjustment_type TEXT NOT NULL CHECK (adjustment_type IN ('CARRYOVER', 'OMITTED_STOCK', 'COUNT_CORRECTION', 'LOSS_OR_DAMAGE', 'OTHER')),
    reason TEXT NOT NULL CHECK (length(trim(reason)) > 0),
    created_at TEXT NOT NULL,
    cash_closure_id INTEGER,
    academic_term_id INTEGER NOT NULL,
    FOREIGN KEY (guide_id) REFERENCES guides(id),
    FOREIGN KEY (cash_closure_id) REFERENCES cash_closures(id),
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
);

CREATE TABLE IF NOT EXISTS authorized_deliveries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guide_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    beneficiary TEXT NOT NULL,
    authorized_by TEXT NOT NULL,
    reason TEXT NOT NULL,
    created_at TEXT NOT NULL,
    academic_term_id INTEGER,
    FOREIGN KEY (guide_id) REFERENCES guides(id),
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
);

CREATE TABLE IF NOT EXISTS authorized_delivery_returns (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    delivery_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    reason TEXT NOT NULL,
    created_at TEXT NOT NULL,
    academic_term_id INTEGER,
    FOREIGN KEY (delivery_id) REFERENCES authorized_deliveries(id),
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
);

CREATE TABLE IF NOT EXISTS order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    guide_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC NOT NULL CHECK (unit_cost >= 0),

    FOREIGN KEY (order_id) REFERENCES orders(id),
    FOREIGN KEY (guide_id) REFERENCES guides(id)
);

CREATE TABLE IF NOT EXISTS cash_closures (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    expected_cash NUMERIC NOT NULL,
    counted_cash NUMERIC NOT NULL,
    expected_qr NUMERIC NOT NULL,
    reported_qr NUMERIC NOT NULL,
    notes TEXT,
    status TEXT NOT NULL CHECK (status IN ('VALID', 'CANCELLED')),
    cancellation_reason TEXT,
    closure_date TEXT NOT NULL,
    created_at TEXT NOT NULL,
    academic_term_id INTEGER,
    FOREIGN KEY (academic_term_id) REFERENCES academic_terms(id)
);

CREATE TABLE IF NOT EXISTS cash_closure_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    cash_closure_id INTEGER NOT NULL,
    guide_id INTEGER NOT NULL,
    expected_stock INTEGER NOT NULL CHECK (expected_stock >= 0),
    counted_stock INTEGER NOT NULL CHECK (counted_stock >= 0),

    FOREIGN KEY (cash_closure_id) REFERENCES cash_closures(id),
    FOREIGN KEY (guide_id) REFERENCES guides(id),
    UNIQUE (cash_closure_id, guide_id)
);

CREATE INDEX IF NOT EXISTS idx_sales_guide_id ON sales(guide_id);

CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales(created_at);

CREATE INDEX IF NOT EXISTS idx_account_movements_account_id ON account_movements(account_id);

CREATE INDEX IF NOT EXISTS idx_account_movements_created_at ON account_movements(created_at);

CREATE INDEX IF NOT EXISTS idx_orders_created_at ON orders(created_at);

CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_created_at ON inventory_adjustments(created_at);

CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_guide_id ON inventory_adjustments(guide_id);

CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_term_id ON inventory_adjustments(academic_term_id);

CREATE INDEX IF NOT EXISTS idx_authorized_deliveries_created_at ON authorized_deliveries(created_at);

CREATE INDEX IF NOT EXISTS idx_authorized_delivery_returns_created_at ON authorized_delivery_returns(created_at);

CREATE INDEX IF NOT EXISTS idx_authorized_delivery_returns_delivery_id ON authorized_delivery_returns(delivery_id);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_guide_id ON order_items(guide_id);

CREATE INDEX IF NOT EXISTS idx_cash_closures_created_at ON cash_closures(created_at);

CREATE INDEX IF NOT EXISTS idx_cash_closure_items_closure_id ON cash_closure_items(cash_closure_id);

CREATE INDEX IF NOT EXISTS idx_cash_closure_items_guide_id ON cash_closure_items(guide_id);
