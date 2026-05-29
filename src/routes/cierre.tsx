import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";

export const Route = createFileRoute("/cierre")({
  component: CierrePage,
  head: () => ({ meta: [{ title: "Cierre de caja · CEF Guías" }] }),
});

function CierrePage() {
  return (
    <AeroShell
      title="Cierre de caja"
      subtitle="Asistente paso a paso — comparar lo contado con lo calculado"
    >
      <div className="grid grid-cols-12 gap-4">
        <Panel title="Paso 1 — Conteo físico de guías" className="col-span-6">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Guía</th>
                <th>Esperado</th>
                <th>Contado</th>
                <th>Diferencia</th>
              </tr>
            </thead>
            <tbody>
              {(["Gral", "I", "II", "III"] as const).map((t, i) => {
                const esperado = [6, 5, 44, 50][i];
                return (
                  <tr key={t}>
                    <td>
                      <GuiaBadge tipo={t} />
                    </td>
                    <td>{esperado}</td>
                    <td>
                      <input className="aero-input w-20 text-center" defaultValue={esperado} />
                    </td>
                    <td className="font-mono text-[oklch(0.4_0.1_250)]">0</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </Panel>

        <Panel title="Paso 2 — Dinero físico contado" className="col-span-6">
          <div className="space-y-2 text-sm">
            <FieldRow k="Esperado (Caja Centro)" v="Bs 231.50" />
            <label className="flex items-center justify-between border-b border-[rgba(120,170,220,0.25)] py-1">
              <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Contado
              </span>
              <input className="aero-input w-32 text-right font-mono" defaultValue="231.50" />
            </label>
            <FieldRow
              k="Diferencia"
              v={<span className="text-[oklch(0.45_0.15_145)] font-bold">Bs 0.00</span>}
            />
          </div>
        </Panel>

        <Panel title="Paso 3 — Verificación QR (Banco)" className="col-span-6">
          <div className="space-y-2 text-sm">
            <FieldRow k="Esperado (Banco)" v="Bs 9 905.00" />
            <FieldRow
              k="Reportado por Soto"
              v={<input className="aero-input w-32 text-right font-mono" defaultValue="9905.00" />}
            />
            <FieldRow
              k="Diferencia"
              v={<span className="text-[oklch(0.45_0.15_145)] font-bold">Bs 0.00</span>}
            />
          </div>
        </Panel>

        <Panel title="Paso 4 — Resultado" className="col-span-6">
          <div className="aero-panel bg-[oklch(0.96_0.06_145)]/40 p-3 text-center">
            <div className="text-2xl">✓</div>
            <div className="font-semibold text-[oklch(0.35_0.15_145)]">Caja cuadra</div>
            <p className="mt-1 text-xs text-[oklch(0.45_0.08_250)]">
              Las 3 cuentas coinciden con lo registrado.
            </p>
          </div>
          <div className="mt-3 flex justify-end gap-2">
            <button className="aero-btn aero-btn-danger px-3 py-1.5 text-sm">
              Registrar pérdida
            </button>
            <button className="aero-btn aero-btn-confirm px-4 py-1.5 text-sm font-semibold">
              Guardar cierre del día
            </button>
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
