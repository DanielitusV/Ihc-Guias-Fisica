insert into guides (name, subject, price, stock)
values
  ('Guia Fisica General', 'Fisica General', 35.00, 100),
  ('Guia Fisica Basica I', 'Fisica Basica I', 35.00, 100),
  ('Guia Fisica Basica II', 'Fisica Basica II', 35.00, 100),
  ('Guia Fisica Basica III', 'Fisica Basica III', 35.00, 100)
on conflict (name) do update
set subject = excluded.subject,
    price = excluded.price,
    stock = excluded.stock;

insert into admins (username, password)
values ('admin', 'admin')
on conflict (username) do update set password = excluded.password;

insert into accounts (name)
values
  ('Cuenta Fisico'),
  ('Cuenta QR')
on conflict (name) do nothing;
