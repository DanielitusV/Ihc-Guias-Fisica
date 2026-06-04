insert into guides (name, subject, price, stock)
select seed.name, seed.subject, seed.price, seed.stock
from (
  values
    ('Fis Gral', 'Fisica General', 30.00, 8),
    ('Fis I', 'Fisica I', 35.00, 15),
    ('Fis II', 'Fisica II', 35.00, 42),
    ('Fis III', 'Fisica III', 40.00, 11)
) as seed(name, subject, price, stock)
where not exists (
  select 1
  from guides
  where guides.name = seed.name
);

insert into accounts (name)
values
  ('Caja fisica'),
  ('Cuenta QR'),
  ('Cuenta encargado')
on conflict (name) do nothing;

insert into inventory_movements (guide_id, type, quantity, note)
select guides.id, 'entrada', guides.stock, 'Carga inicial de prueba'
from guides
where guides.name in ('Fis Gral', 'Fis I', 'Fis II', 'Fis III')
  and not exists (
    select 1
    from inventory_movements
    where inventory_movements.guide_id = guides.id
      and inventory_movements.note = 'Carga inicial de prueba'
  );

insert into account_movements (account_id, type, amount, note)
select accounts.id, 'ingreso', seed.amount, 'Saldo inicial de prueba'
from (
  values
    ('Caja fisica', 280.00),
    ('Cuenta QR', 950.00),
    ('Cuenta encargado', 120.00)
) as seed(name, amount)
join accounts on accounts.name = seed.name
where not exists (
  select 1
  from account_movements
  where account_movements.account_id = accounts.id
    and account_movements.note = 'Saldo inicial de prueba'
);

insert into orders (supplier, status, total_cost)
select seed.supplier, seed.status, seed.total_cost
from (
  values
    ('Fotocopiadora central', 'pendiente', 520.00),
    ('Fotocopiadora central', 'recibido', 380.00)
) as seed(supplier, status, total_cost)
where not exists (
  select 1
  from orders
  where orders.supplier = seed.supplier
    and orders.status = seed.status
    and orders.total_cost = seed.total_cost
);

insert into cash_closures (physical_cash, qr_amount, note)
select 260.00, 900.00, 'Cierre de prueba'
where not exists (
  select 1
  from cash_closures
  where note = 'Cierre de prueba'
);
