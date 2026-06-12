import { useEffect, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, Panel } from "@/components/aero-shell";
import {
  accountKeyByName,
  accountNames,
  accountSubs,
  ensureAccounts,
  signedAmount,
  type AccountMovement,
  type AccountRow,
  type Cuenta,
} from "@/lib/accounts";
import { supabase } from "@/lib/supabase";

export const Route = createFileRoute("/cuentas")({
  component: CuentasPage,
  head: () => ({ meta: [{ title: "Cuentas - CEF Guias" }] }),
});

type Mov = { fecha: string; concepto: string; monto: number };
type AccountState = {
  id: number;
  nombre: string;
  sub: string;
  saldo: number;
  movs: Mov[];
};


const createEmptyAccounts = (): Record<Cuenta, AccountState> => ({
  centro: { id: 0, nombre: accountNames.centro, sub: accountSubs.centro, saldo: 0, movs: [] },
  banco: { id: 0, nombre: accountNames.banco, sub: accountSubs.banco, saldo: 0, movs: [] },
  encargado: { id: 0, nombre: accountNames.encargado, sub: accountSubs.encargado, saldo: 0, movs: [] },
});

const formatMoney = (value: number) =>
  `Bs ${value.toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

const formatMonto = (value: number) => {
  const sign = value >= 0 ? "+" : "-";
  return `${sign}${Math.abs(value).toFixed(2)}`;
};

const formatDate = (value: string) => {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return `${date.getDate().toString().padStart(2, "0")}/${(date.getMonth() + 1)
    .toString()
    .padStart(2, "0")}`;
};


const buildAccountState = (
  accounts: AccountRow[],
  movements: AccountMovement[],
): Record<Cuenta, AccountState> => {
  const state = createEmptyAccounts();

  accounts.forEach((account) => {
    const key = accountKeyByName(account.name);
    if (key) state[key].id = account.id;
  });

  movements.forEach((movement) => {
    const entry = (Object.entries(state) as [Cuenta, AccountState][]).find(
      ([, account]) => account.id === movement.account_id,
    );
    if (!entry) return;
    const [, account] = entry;
    account.movs.push({
      fecha: formatDate(movement.created_at),
      concepto: movement.note || movement.type,
      monto: signedAmount(movement.type, Number(movement.amount)),
    });
    account.saldo += signedAmount(movement.type, Number(movement.amount));
  });

  return state;
};

function CuentasPage() {
  const [tab, setTab] = useState<Cuenta>("centro");
  const [accounts, setAccounts] = useState<Record<Cuenta, AccountState>>(createEmptyAccounts());
  const [tipo, setTipo] = useState<"entrada" | "salida" | "retiro">("entrada");
  const [monto, setMonto] = useState("");
  const [concepto, setConcepto] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [providerDebt, setProviderDebt] = useState(0);

  const currentAccount = accounts[tab];

  useEffect(() => {
    loadAccounts();
  }, []);

  const loadAccounts = async () => {
    setLoading(true);
    setError(null);
    try {
      const existingAccounts = await ensureAccounts();

      const accountIds = existingAccounts.map((a) => a.id).filter(Boolean);
      const [{ data: movements, error: movementsError }, { data: pendingOrders, error: ordersError }] =
        await Promise.all([
          supabase
            .from("account_movements")
            .select("id,account_id,type,amount,note,created_at")
            .in("account_id", accountIds)
            .order("created_at", { ascending: false }),
          supabase.from("orders").select("total_cost,status"),
        ]);

      if (movementsError) throw movementsError;
      if (ordersError) throw ordersError;

      setAccounts(buildAccountState(existingAccounts, movements ?? []));
      setProviderDebt(
        (pendingOrders ?? [])
          .filter((order) => String(order.status).toLowerCase() !== "pagado")
          .reduce((sum, order) => sum + Number(order.total_cost), 0),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
      setAccounts(createEmptyAccounts());
      setProviderDebt(0);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async () => {
    const parsed = Number(monto.replace(",", "."));
    if (Number.isNaN(parsed) || parsed <= 0) {
      setError("El monto debe ser mayor a 0.");
      return;
    }
    if (!concepto.trim()) {
      setError("El concepto es obligatorio.");
      return;
    }
    if (!currentAccount.id) {
      setError("No se encontro la cuenta seleccionada. Recarga la pagina.");
      return;
    }
    if (tipo === "retiro" && tab !== "centro") {
      setError("El retiro solo se realiza desde Caja del Centro.");
      return;
    }
    if (tipo === "retiro" && !accounts.encargado.id) {
      setError("No se encontro la cuenta del encargado. Recarga la pagina.");
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const payload =
        tipo === "retiro"
          ? [
              {
                account_id: currentAccount.id,
                type: "retiro",
                amount: parsed,
                note: concepto.trim(),
              },
              {
                account_id: accounts.encargado.id,
                type: "ingreso",
                amount: parsed,
                note: `Retiro desde centro - ${concepto.trim()}`,
              },
            ]
          : [
              {
                account_id: currentAccount.id,
                type: tipo === "entrada" ? "ingreso" : "salida",
                amount: parsed,
                note: concepto.trim(),
              },
            ];

      const { error: insertError } = await supabase.from("account_movements").insert(payload);
      if (insertError) throw insertError;

      setMonto("");
      setConcepto("");
      // FIX: resetear tipo a "entrada" despues de un retiro para evitar confusion
      if (tipo === "retiro") setTipo("entrada");

      await loadAccounts();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      // FIX: saving siempre se resetea, incluso si hubo error
      setSaving(false);
    }
  };

  // FIX: al cambiar de tab, resetear tipo a "entrada" si el nuevo tab no es centro
  const handleSwitchTab = (k: Cuenta) => {
    setTab(k);
    setError(null);
    if (k !== "centro" && tipo === "retiro") setTipo("entrada");
  };

  const handleRetiro = () => {
    setTipo("retiro");
    setConcepto("Retiro a encargado");
  };

  return (
    <AeroShell
      title="Cuentas (3)"
      subtitle="Cada movimiento se registra en una sola cuenta. Los retiros mueven dinero Centro -> Encargado."
      interactive
    >
      <div className="mb-3 flex gap-1">
        {(Object.keys(accounts) as Cuenta[]).map((k) => (
          <button
            key={k}
            onClick={() => handleSwitchTab(k)}
            className={`aero-btn px-4 py-2 text-sm ${tab === k ? "font-semibold shadow-inner" : ""}`}
          >
            {accounts[k].nombre}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-12 gap-4">
        <Panel title={currentAccount.nombre} hint={currentAccount.sub} className="col-span-8">
          {loading ? (
            <div className="p-8 text-center text-sm text-[oklch(0.35_0.12_250)]">
              Cargando movimientos...
            </div>
          ) : (
            <table className="aero-table">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Concepto</th>
                  <th className="text-right">Monto (Bs)</th>
                </tr>
              </thead>
              <tbody>
                {currentAccount.movs.length === 0 ? (
                  <tr>
                    <td colSpan={3} className="py-4 text-center text-sm text-[oklch(0.45_0.08_250)]">
                      Sin movimientos registrados
                    </td>
                  </tr>
                ) : (
                  currentAccount.movs.map((m, i) => (
                    <tr key={i}>
                      <td className="font-mono text-xs">{m.fecha}</td>
                      <td>{m.concepto}</td>
                      <td
                        className={`text-right font-mono font-semibold ${
                          m.monto >= 0
                            ? "text-[oklch(0.45_0.15_145)]"
                            : "text-[oklch(0.5_0.2_25)]"
                        }`}
                      >
                        {formatMonto(m.monto)}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          )}
        </Panel>

        <div className="col-span-4 space-y-4">
          <Panel title="Saldo actual">
            <div className="py-2 text-center">
              <div className="text-[11px] uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Disponible
              </div>
              <div
                className={`font-mono text-3xl font-bold ${
                  currentAccount.saldo < 0
                    ? "text-[oklch(0.5_0.2_25)]"
                    : "text-[oklch(0.3_0.16_245)]"
                }`}
              >
                {formatMoney(currentAccount.saldo)}
              </div>
            </div>
          </Panel>

          <Panel title="Registrar movimiento">
            <div className="space-y-3 text-sm">
              <label className="block">
                <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                  Tipo
                </span>
                <select
                  value={tipo}
                  onChange={(e) => setTipo(e.target.value as "entrada" | "salida" | "retiro")}
                  className="aero-input mt-1 w-full"
                  disabled={saving}
                >
                  <option value="entrada">Ingreso</option>
                  <option value="salida">Salida</option>
                  {/* FIX: retiro solo disponible en tab centro */}
                  {tab === "centro" && (
                    <option value="retiro">Retiro encargado</option>
                  )}
                </select>
              </label>
              <label className="block">
                <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                  Monto
                </span>
                <input
                  value={monto}
                  onChange={(e) => setMonto(e.target.value)}
                  placeholder="0.00"
                  className="aero-input mt-1 w-full text-right"
                  disabled={saving}
                  type="number"
                  min="0.01"
                  step="0.01"
                />
              </label>
              <label className="block">
                <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                  Concepto
                </span>
                <input
                  value={concepto}
                  onChange={(e) => setConcepto(e.target.value)}
                  placeholder="Descripcion breve"
                  className="aero-input mt-1 w-full"
                  disabled={saving}
                  maxLength={120}
                />
              </label>
              <button
                onClick={handleSubmit}
                className="aero-btn aero-btn-confirm w-full py-2 text-sm font-semibold"
                disabled={saving}
              >
                {saving ? "Guardando..." : "Guardar movimiento"}
              </button>
              {error && (
                <div className="rounded border border-[oklch(0.8_0.1_25)] bg-[oklch(0.97_0.03_25)] px-3 py-2 text-sm text-[oklch(0.5_0.2_25)]">
                  {error}
                </div>
              )}
            </div>
          </Panel>

          <Panel title="Acciones rapidas">
            <div className="space-y-2">
              <button
                onClick={() => setTipo("entrada")}
                className="aero-btn w-full py-2 text-sm"
                disabled={saving}
              >
                + Registrar entrada
              </button>
              <button
                onClick={() => setTipo("salida")}
                className="aero-btn w-full py-2 text-sm"
                disabled={saving}
              >
                - Registrar salida
              </button>
              {tab === "centro" && (
                <button
                  onClick={handleRetiro}
                  className="aero-btn w-full py-2 text-sm"
                  disabled={saving}
                >
                  Retiro encargado
                </button>
              )}
            </div>
          </Panel>

          {tab === "encargado" && (
            <Panel title="Deuda fotocopiadora">
              <div className="py-2 text-center">
                <div className="text-[11px] uppercase tracking-wide">Pendiente</div>
                <div className="font-mono text-2xl font-bold text-[oklch(0.5_0.2_25)]">
                  {formatMoney(providerDebt)}
                </div>
                <p className="mt-1 text-[11px] text-[oklch(0.45_0.08_250)]">
                  Al pagar, registrar como salida en Encargado.
                </p>
              </div>
            </Panel>
          )}
        </div>
      </div>
    </AeroShell>
  );
}
