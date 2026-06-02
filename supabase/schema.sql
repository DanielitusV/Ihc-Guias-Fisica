create table guides (
  id bigint generated always as identity primary key,
  name text not null,
  subject text not null,
  price numeric(10, 2) not null default 0,
  stock integer not null default 0,
  created_at timestamptz not null default now()
);

create table inventory_movements (
  id bigint generated always as identity primary key,
  guide_id bigint not null references guides(id),
  type text not null check (type in ('entrada', 'salida', 'ajuste')),
  quantity integer not null,
  note text,
  created_at timestamptz not null default now()
);

create table accounts (
  id bigint generated always as identity primary key,
  name text not null unique
);

create table account_movements (
  id bigint generated always as identity primary key,
  account_id bigint not null references accounts(id),
  type text not null check (type in ('ingreso', 'salida', 'retiro')),
  amount numeric(10, 2) not null,
  note text,
  created_at timestamptz not null default now()
);

create table orders (
  id bigint generated always as identity primary key,
  supplier text not null,
  status text not null default 'pendiente',
  total_cost numeric(10, 2) not null default 0,
  created_at timestamptz not null default now()
);

create table cash_closures (
  id bigint generated always as identity primary key,
  physical_cash numeric(10, 2) not null default 0,
  qr_amount numeric(10, 2) not null default 0,
  note text,
  created_at timestamptz not null default now()
);

alter table guides enable row level security;
alter table inventory_movements enable row level security;
alter table accounts enable row level security;
alter table account_movements enable row level security;
alter table orders enable row level security;
alter table cash_closures enable row level security;
