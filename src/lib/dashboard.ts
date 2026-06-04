import { supabase } from "@/lib/supabase";

export type GuideStock = {
  id: number;
  name: string;
  subject: string;
  price: number;
  stock: number;
};

export type AccountSummary = {
  id: number;
  name: string;
  balance: number;
};

export type RecentMovement = {
  id: string;
  date: string;
  source: "Inventario" | "Cuenta";
  title: string;
  detail: string;
};

export type DashboardData = {
  guides: GuideStock[];
  accounts: AccountSummary[];
  recentMovements: RecentMovement[];
};

type InventoryMovementRow = {
  id: number;
  type: string;
  quantity: number;
  note: string | null;
  created_at: string;
  guides: { name: string } | { name: string }[] | null;
};

type AccountMovementRow = {
  id: number;
  type: string;
  amount: number;
  note: string | null;
  created_at: string;
  accounts: { name: string } | { name: string }[] | null;
};

function relationName(relation: { name: string } | { name: string }[] | null) {
  if (Array.isArray(relation)) return relation[0]?.name;
  return relation?.name;
}

export async function getDashboardData(): Promise<DashboardData> {
  const [guidesResult, accountsResult, inventoryResult, accountMovementsResult] = await Promise.all(
    [
      supabase.from("guides").select("id,name,subject,price,stock").order("id"),
      supabase.from("accounts").select("id,name").order("name"),
      supabase
        .from("inventory_movements")
        .select("id,type,quantity,note,created_at,guides(name)")
        .order("created_at", { ascending: false })
        .limit(8),
      supabase
        .from("account_movements")
        .select("id,type,amount,note,created_at,accounts(name)")
        .order("created_at", { ascending: false }),
    ],
  );

  const firstError =
    guidesResult.error ??
    accountsResult.error ??
    inventoryResult.error ??
    accountMovementsResult.error;

  if (firstError) {
    throw new Error(firstError.message);
  }

  const guides = (guidesResult.data ?? []).map((guide) => ({
    id: Number(guide.id),
    name: String(guide.name),
    subject: String(guide.subject),
    price: Number(guide.price),
    stock: Number(guide.stock),
  }));

  const accountRows = accountsResult.data ?? [];
  const accountMovementRows = (accountMovementsResult.data ?? []) as AccountMovementRow[];

  const accounts = accountRows.map((account) => {
    const accountName = String(account.name);
    const balance = accountMovementRows
      .filter((movement) => relationName(movement.accounts) === accountName)
      .reduce((total, movement) => {
        const amount = Number(movement.amount);
        return movement.type === "salida" || movement.type === "retiro"
          ? total - amount
          : total + amount;
      }, 0);

    return {
      id: Number(account.id),
      name: accountName,
      balance,
    };
  });

  const inventoryMovements = ((inventoryResult.data ?? []) as InventoryMovementRow[]).map(
    (movement) => ({
      id: `inventory-${movement.id}`,
      date: movement.created_at,
      source: "Inventario" as const,
      title: relationName(movement.guides) ?? "Guia sin nombre",
      detail: `${movement.type} de ${movement.quantity} unidad(es)${
        movement.note ? ` - ${movement.note}` : ""
      }`,
    }),
  );

  const moneyMovements = accountMovementRows.map((movement) => ({
    id: `account-${movement.id}`,
    date: movement.created_at,
    source: "Cuenta" as const,
    title: relationName(movement.accounts) ?? "Cuenta sin nombre",
    detail: `${movement.type} de Bs ${Number(movement.amount).toFixed(2)}${
      movement.note ? ` - ${movement.note}` : ""
    }`,
  }));

  const recentMovements = [...inventoryMovements, ...moneyMovements]
    .sort((left, right) => new Date(right.date).getTime() - new Date(left.date).getTime())
    .slice(0, 10);

  return { guides, accounts, recentMovements };
}
