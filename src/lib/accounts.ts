import { supabase } from "@/lib/supabase";

export type Cuenta = "centro" | "banco" | "encargado";
export type AccountMovementType = "ingreso" | "salida" | "retiro";

export type AccountRow = {
  id: number;
  name: string;
};

export type AccountMovement = {
  id: number;
  account_id: number;
  type: AccountMovementType;
  amount: number;
  note: string | null;
  created_at: string;
};

export const accountNames: Record<Cuenta, string> = {
  centro: "Cuenta Fisico",
  banco: "Cuenta QR",
  encargado: "Cuenta Encargado",
};

export const accountSubs: Record<Cuenta, string> = {
  centro: "Dinero fisico recibido en el centro",
  banco: "Pagos recibidos por QR",
  encargado: "Dinero retirado del centro para pagar a fotocopiadora",
};

export const accountKeys = Object.keys(accountNames) as Cuenta[];

export function accountKeyByName(name: string): Cuenta | undefined {
  return accountKeys.find((key) => accountNames[key] === name);
}

export function signedAmount(type: AccountMovementType, amount: number) {
  const abs = Math.abs(Number(amount) || 0);
  return type === "ingreso" ? abs : -abs;
}

export async function ensureAccounts(): Promise<AccountRow[]> {
  const names = Object.values(accountNames);

  const { data: existing, error: existingError } = await supabase
    .from("accounts")
    .select("id,name")
    .in("name", names);
  if (existingError) throw existingError;

  const found = existing ?? [];
  const missing = names.filter((name) => !found.some((account) => account.name === name));

  for (const name of missing) {
    const { error } = await supabase.from("accounts").insert({ name });
    if (error && error.code !== "23505") throw error;
  }

  const { data: refetch, error: refetchError } = await supabase
    .from("accounts")
    .select("id,name")
    .in("name", names)
    .order("name");
  if (refetchError) throw refetchError;

  return refetch ?? [];
}

export function accountIdByKey(accounts: AccountRow[], key: Cuenta) {
  return accounts.find((account) => account.name === accountNames[key])?.id ?? 0;
}
