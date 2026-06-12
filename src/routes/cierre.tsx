import { useEffect, useMemo, useRef, useState } from "react";
import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";
import { fetchGuides, type GuiaTipo } from "@/lib/data";
import { accountIdByKey, accountNames, ensureAccounts, signedAmount } from "@/lib/accounts";
import { supabase } from "@/lib/supabase";

export const Route = createFileRoute("/cierre")({
  component: CierrePage,
  head: () => ({ meta: [{ title: "Cierre de caja - CEF Guias" }] }),
});

type AccountMovement = { account_id: number; amount: number; type: "ingreso" | "salida" | "retiro" };
type CashClosure = {
  id: number;
  physical_cash: number;
  qr_amount: number;
  note: string | null;
  created_at: string;
};


const GUIDE_TYPES = ["Gral", "I", "II", "III"] as const;
const EMPTY_GUIDE_COUNTS: Record<GuiaTipo, number> = { Gral: 0, I: 0, II: 0, III: 0 };

const formatMoney = (value: number) =>
  `Bs ${value.toLocaleString("es-BO", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

function CierrePage() {
  const [expectedGuides, setExpectedGuides] = useState<Record<GuiaTipo, number>>({
    ...EMPTY_GUIDE_COUNTS,
  });
  const [counts, setCounts] = useState<Record<GuiaTipo, number>>({ ...EMPTY_GUIDE_COUNTS });
  const [contadoCentro, setContadoCentro] = useState(0);
  const [reportadoBanco, setReportadoBanco] = useState(0);
  const [expectedCentro, setExpectedCentro] = useState(0);
  const [expectedBanco, setExpectedBanco] = useState(0);
  const [lastClosure, setLastClosure] = useState<CashClosure | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // FIX: ref para saber si los campos ya fueron tocados por el usuario
  const userEditedCentro = useRef(false);
  const userEditedBanco = useRef(false);
  const userEditedGuides = useRef(false);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [accounts, guides] = await Promise.all([ensureAccounts(), fetchGuides()]);

      const guideCounts = guides.reduce<Record<GuiaTipo, number>>(
        (acc, guide) => ({
          ...acc,
          [guide.tipo]: Number(guide.stock) || 0,
        }),
        { ...EMPTY_GUIDE_COUNTS },
      );

      setExpectedGuides(guideCounts);
      if (!userEditedGuides.current) setCounts(guideCounts);

      const centroId = accountIdByKey(accounts, "centro");
      const bancoId = accountIdByKey(accounts, "banco");
      const accountIds = [centroId, bancoId].filter(Boolean);

      let saldoCentro = 0;
      let saldoBanco = 0;

      if (accountIds.length > 0) {
        const { data: movements, error: movementsError } = await supabase
          .from("account_movements")
          .select("account_id,amount,type")
          .in("account_id", accountIds);
        if (movementsError) throw movementsError;

        saldoCentro =
          movements
            ?.filter((m) => m.account_id === centroId)
            .reduce((sum, m) => sum + signedAmount(m.type, Number(m.amount)), 0) ?? 0;
        saldoBanco =
          movements
            ?.filter((m) => m.account_id === bancoId)
            .reduce((sum, m) => sum + signedAmount(m.type, Number(m.amount)), 0) ?? 0;
      }

      setExpectedCentro(saldoCentro);
      setExpectedBanco(saldoBanco);

      // FIX: solo inicializar los campos contados si el usuario no los edito manualmente
      if (!userEditedCentro.current) setContadoCentro(saldoCentro);
      if (!userEditedBanco.current) setReportadoBanco(saldoBanco);

      const { data: closures, error: closuresError } = await supabase
        .from("cash_closures")
        .select("id,physical_cash,qr_amount,note,created_at")
        .order("created_at", { ascending: false })
        .limit(1);
      if (closuresError) throw closuresError;
      setLastClosure(closures?.[0] ?? null);
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  };

  const handleSaveClosure = async () => {
    setSaving(true);
    setError(null);
    try {
      const { error: insertError } = await supabase.from("cash_closures").insert([
        {
          physical_cash: contadoCentro,
          qr_amount: reportadoBanco,
          note: "Cierre manual desde UI",
        },
      ]);
      if (insertError) throw insertError;

      // FIX: resetear flags de edicion para que loadData actualice los campos
      userEditedCentro.current = false;
      userEditedBanco.current = false;
      userEditedGuides.current = false;
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setSaving(false);
    }
  };

  const guideRows = useMemo(
    () =>
      GUIDE_TYPES.map((tipo) => ({
        tipo,
        esperado: expectedGuides[tipo],
        contado: counts[tipo],
        diff: counts[tipo] - expectedGuides[tipo],
      })),
    [counts, expectedGuides],
  );

  const difCentro = contadoCentro - expectedCentro;
  const difBanco = reportadoBanco - expectedBanco;
  const cajaCuadra = useMemo(
    () =>
      Math.abs(difCentro) < 0.005 &&
      Math.abs(difBanco) < 0.005 &&
      guideRows.every((r) => r.diff === 0),
    [difCentro, difBanco, guideRows],
  );

  if (loading) {
    return (
      <AeroShell title="Cierre de caja">
        <div className="p-8 text-center text-sm text-[oklch(0.35_0.12_250)]">
          Cargando datos...
        </div>
      </AeroShell>
    );
  }

  return (
    <AeroShell
      title="Cierre de caja"
      subtitle="Asistente paso a paso - comparar lo contado con lo calculado"
      interactive
    >
      <div className="grid grid-cols-12 gap-4">
        {/* Paso 1 */}
        <Panel title="Paso 1 - Conteo fisico de guias" className="col-span-6">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Guia</th>
                <th>Esperado</th>
                <th>Contado</th>
                <th>Diferencia</th>
              </tr>
            </thead>
            <tbody>
              {guideRows.map(({ tipo, esperado, contado, diff }) => (
                <tr key={tipo}>
                  <td>
                    <GuiaBadge tipo={tipo} />
                  </td>
                  <td>{esperado}</td>
                  <td>
                    <input
                      className="aero-input w-20 text-center"
                      type="number"
                      min={0}
                      step={1}
                      value={contado}
                      onChange={(e) =>
                        {
                          userEditedGuides.current = true;
                          setCounts((prev) => ({
                            ...prev,
                            [tipo]: Number(e.target.value),
                          }));
                        }
                      }
                      disabled={saving}
                    />
                  </td>
                  <td
                    className={`font-mono ${
                      diff === 0
                        ? "text-[oklch(0.4_0.1_250)]"
                        : diff > 0
                          ? "text-[oklch(0.45_0.15_145)]"
                          : "text-[oklch(0.5_0.2_25)]"
                    }`}
                  >
                    {diff > 0 ? `+${diff}` : diff.toString()}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        {/* Paso 2 */}
        <Panel title="Paso 2 - Dinero fisico contado" className="col-span-6">
          <div className="space-y-2 text-sm">
            <FieldRow k="Esperado (Caja Centro)" v={formatMoney(expectedCentro)} />
            <label className="flex items-center justify-between border-b border-[rgba(120,170,220,0.25)] py-1">
              <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Contado
              </span>
              <input
                className="aero-input w-32 text-right font-mono"
                type="number"
                min={0}
                step="0.01"
                value={contadoCentro}
                onChange={(e) => {
                  userEditedCentro.current = true;
                  setContadoCentro(Number(e.target.value));
                }}
                disabled={saving}
              />
            </label>
            <FieldRow
              k="Diferencia"
              v={
                <span
                  className={`font-bold ${
                    Math.abs(difCentro) < 0.005
                      ? "text-[oklch(0.45_0.15_145)]"
                      : "text-[oklch(0.5_0.2_25)]"
                  }`}
                >
                  {formatMoney(difCentro)}
                </span>
              }
            />
          </div>
        </Panel>

        {/* Paso 3 */}
        <Panel title="Paso 3 - Verificacion QR (Banco)" className="col-span-6">
          <div className="space-y-2 text-sm">
            <FieldRow k="Esperado (Banco)" v={formatMoney(expectedBanco)} />
            <FieldRow
              k="Reportado por Soto"
              v={
                <input
                  className="aero-input w-32 text-right font-mono"
                  type="number"
                  min={0}
                  step="0.01"
                  value={reportadoBanco}
                  onChange={(e) => {
                    userEditedBanco.current = true;
                    setReportadoBanco(Number(e.target.value));
                  }}
                  disabled={saving}
                />
              }
            />
            <FieldRow
              k="Diferencia"
              v={
                <span
                  className={`font-bold ${
                    Math.abs(difBanco) < 0.005
                      ? "text-[oklch(0.45_0.15_145)]"
                      : "text-[oklch(0.5_0.2_25)]"
                  }`}
                >
                  {formatMoney(difBanco)}
                </span>
              }
            />
          </div>
        </Panel>

        {/* Paso 4 */}
        <Panel title="Paso 4 - Resultado" className="col-span-6">
          <div
            className={`aero-panel p-3 text-center ${
              cajaCuadra
                ? "bg-[oklch(0.96_0.06_145)]/40"
                : "bg-[oklch(0.97_0.06_25)]/40"
            }`}
          >
            <div className="text-2xl">{cajaCuadra ? "OK" : "!"}</div>
            <div
              className={`font-semibold ${
                cajaCuadra
                  ? "text-[oklch(0.35_0.15_145)]"
                  : "text-[oklch(0.45_0.18_25)]"
              }`}
            >
              {cajaCuadra ? "Caja cuadra" : "Caja no cuadra"}
            </div>
            <p className="mt-1 text-xs text-[oklch(0.45_0.08_250)]">
              {cajaCuadra
                ? "Las cuentas coinciden con lo registrado."
                : "Revisa los valores antes de guardar el cierre."}
            </p>
          </div>

          <div className="mt-3 flex flex-col gap-2">
            <button
              className="aero-btn aero-btn-danger px-3 py-1.5 text-sm"
              disabled={saving}
              onClick={() => {
                // Registrar diferencia negativa como perdida en la misma pagina
                if (difCentro < 0 || difBanco < 0) {
                  alert(
                    `Diferencia Centro: ${formatMoney(difCentro)}\nDiferencia Banco: ${formatMoney(difBanco)}\n\nRegistra la perdida como una salida en la pagina de Cuentas.`,
                  );
                }
              }}
            >
              Registrar perdida
            </button>
            <button
              onClick={handleSaveClosure}
              className="aero-btn aero-btn-confirm px-4 py-1.5 text-sm font-semibold"
              disabled={saving}
            >
              {saving ? "Guardando..." : "Guardar cierre del dia"}
            </button>

            {error && (
              <div className="rounded border border-[oklch(0.8_0.1_25)] bg-[oklch(0.97_0.03_25)] px-3 py-2 text-sm text-[oklch(0.5_0.2_25)]">
                {error}
              </div>
            )}

            {lastClosure && (
              <div className="rounded border border-[rgba(120,170,220,0.4)] bg-white/80 p-3 text-sm">
                <div className="font-semibold">Ultimo cierre</div>
                <div className="flex justify-between text-xs text-[oklch(0.45_0.08_250)]">
                  <span>Caja fisica</span>
                  <span className="font-mono">{formatMoney(lastClosure.physical_cash)}</span>
                </div>
                <div className="flex justify-between text-xs text-[oklch(0.45_0.08_250)]">
                  <span>QR Banco</span>
                  <span className="font-mono">{formatMoney(lastClosure.qr_amount)}</span>
                </div>
                <div className="mt-1 text-[11px] text-[oklch(0.55_0.07_250)]">
                  {new Date(lastClosure.created_at).toLocaleString("es-BO")}
                </div>
              </div>
            )}
          </div>
        </Panel>
      </div>
    </AeroShell>
  );
}

function FieldRow({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-[rgba(120,170,220,0.25)] py-1">
      <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">{k}</span>
      <span className="font-mono">{v}</span>
    </div>
  );
}
