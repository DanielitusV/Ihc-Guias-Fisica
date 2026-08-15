CREATE TABLE IF NOT EXISTS guides (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE,
    current_price NUMERIC NOT NULL CHECK (current_price > 0),
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
    price NUMERIC NOT NULL CHECK (price > 0),
    payment_method TEXT NOT NULL CHECK (payment_method IN ('CASH', 'QR')),
    status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
    cancellation_reason TEXT,
    created_at TEXT NOT NULL,

    FOREIGN KEY (guide_id) REFERENCES guides(id)
);

CREATE TABLE IF NOT EXISTS account_movements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    concept TEXT NOT NULL CHECK (concept IN ('SALE', 'GENERAL_EXPENSE', 'SUPPLIER_PAYMENT', 'SALE_CANCELLATION', 'CLOSURE_ADJUSTMENT', 'OTHER')),
    amount NUMERIC NOT NULL CHECK (amount > 0),
    reason TEXT,
    created_at TEXT NOT NULL,

    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE TABLE IF NOT EXISTS orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    payment_condition TEXT NOT NULL CHECK (payment_condition IN ('PAID', 'CREDIT')),
    created_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    order_id INTEGER NOT NULL,
    guide_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_cost NUMERIC NOT NULL CHECK (unit_cost > 0),

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
    created_at TEXT NOT NULL
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

CREATE INDEX IF NOT EXISTS idx_order_items_order_id ON order_items(order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_guide_id ON order_items(guide_id);

CREATE INDEX IF NOT EXISTS idx_cash_closures_created_at ON cash_closures(created_at);

CREATE INDEX IF NOT EXISTS idx_cash_closure_items_closure_id ON cash_closure_items(cash_closure_id);

CREATE INDEX IF NOT EXISTS idx_cash_closure_items_guide_id ON cash_closure_items(guide_id);
