import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";

export const Route = createFileRoute("/")({
  component: Dashboard,
  head: () => ({ meta: [{ title: "Dashboard · CEF Guías" }] }),
});

const guias = [
  { tipo: "Gral" as const, stock: 6 },
  { tipo: "I" as const, stock: 5 },
  { tipo: "II" as const, stock: 44 },
  { tipo: "III" as const, stock: 50 },
];

const ultimos = [
  { hora: "13:20", guia: "I" as const, pago: "Efectivo", cuenta: "Centro" },
  { hora: "11:27", guia: "I" as const, pago: "QR", cuenta: "Banco" },
  { hora: "15:58", guia: "II" as const, pago: "Efectivo", cuenta: "Centro" },
  { hora: "17:27", guia: "I" as const, pago: "Efectivo", cuenta: "Centro" },
  { hora: "17:18", guia: "III" as const, pago: "Efectivo", cuenta: "Centro" },
  { hora: "11:40", guia: "Gral" as const, pago: "QR", cuenta: "Banco" },
];

function Dashboard() {
  return (
    <AeroShell title="Dashboard" subtitle="Panel principal — operación diaria del centro">
      <div className="grid grid-cols-12 gap-4">
        {/* Ventas rápidas */}
        <Panel title="Registrar ventas rápidas" hint="Un botón = 1 guía" className="col-span-8">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Guía</th>
                <th>Stock</th>
                <th>Efectivo → Centro</th>
                <th>QR → Banco</th>
              </tr>
            </thead>
            <tbody>
              {guias.map((g) => (
                <tr key={g.tipo}>
                  <td>
                    <GuiaBadge tipo={g.tipo} />
                  </td>
                  <td
                    className={
                      g.stock < 10 ? "font-bold text-[oklch(0.5_0.2_25)]" : "font-semibold"
                    }
                  >
                    {g.stock}
                  </td>
                  <td>
                    <button className="aero-btn px-4 py-1.5 text-sm font-medium">+ Vender</button>
                  </td>
                  <td>
                    <button className="aero-btn px-4 py-1.5 text-sm font-medium">
                      + Vender QR
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        {/* 3 cajas — resumen */}
        <Panel
          title="Resumen de las 3 cuentas"
          hint="Centro · Banco · Encargado"
          className="col-span-4"
        >
          <div className="space-y-2">
            <CajaRow
              nombre="Caja del Centro (Físico)"
              monto="Bs 231.50"
              sub="Efectivo en el centro"
            />
            <CajaRow nombre="Cuenta Banco (QR)" monto="Bs 9 905.00" sub="Pagos QR · Soto" />
            <CajaRow
              nombre="Cuenta Encargado"
              monto="Bs 1 240.00"
              sub="Retiros pendientes de pago a fotocopiadora"
            />
            <div className="mt-2 border-t border-[rgba(120,170,220,0.4)] pt-2 text-xs">
              <div className="flex justify-between">
                <span>Gastos del mes</span>
                <span>Bs 32 108.50</span>
              </div>
              <div className="flex justify-between font-semibold text-[oklch(0.5_0.2_25)]">
                <span>Deuda fotocopiadora</span>
                <span>Bs 27 243.68</span>
              </div>
            </div>
          </div>
        </Panel>

        {/* Ventas semana */}
        <Panel title="Ventas semana actual" hint="Lun a Sáb" className="col-span-8">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Guía</th>
                <th>Lun</th>
                <th>Ma</th>
                <th>Mi</th>
                <th>Jue</th>
                <th>Vie</th>
                <th>Sab</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>
                  <GuiaBadge tipo="Gral" />
                </td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td className="font-semibold">0</td>
              </tr>
              <tr>
                <td>
                  <GuiaBadge tipo="I" />
                </td>
                <td>2</td>
                <td></td>
                <td>1</td>
                <td>2</td>
                <td></td>
                <td></td>
                <td className="font-semibold">5</td>
              </tr>
              <tr>
                <td>
                  <GuiaBadge tipo="II" />
                </td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td className="font-semibold">0</td>
              </tr>
              <tr>
                <td>
                  <GuiaBadge tipo="III" />
                </td>
                <td>1</td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td></td>
                <td className="font-semibold">1</td>
              </tr>
            </tbody>
          </table>
        </Panel>

        {/* Últimos */}
        <Panel title="Últimos 10 registros" hint="Historial rápido" className="col-span-4">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Hora</th>
                <th>Guía</th>
                <th>Pago</th>
              </tr>
            </thead>
            <tbody>
              {ultimos.map((u, i) => (
                <tr key={i}>
                  <td className="font-mono text-xs">{u.hora}</td>
                  <td>
                    <GuiaBadge tipo={u.guia} />
                  </td>
                  <td className="text-xs">{u.pago}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        {/* Acciones rápidas */}
        <Panel
          title="Salida de dinero (Centro)"
          hint="Gasto / pago / pérdida"
          className="col-span-5"
        >
          <div className="space-y-2 text-sm">
            <Field label="Fecha">
              <input type="date" className="aero-input w-full" defaultValue="2026-05-26" />
            </Field>
            <Field label="Monto (Bs)">
              <input className="aero-input w-full" placeholder="0.00" />
            </Field>
            <Field label="Motivo">
              <input className="aero-input w-full" placeholder="Pago agua, pérdida, gastos…" />
            </Field>
            <div className="flex justify-end gap-2 pt-1">
              <button className="aero-btn aero-btn-danger px-3 py-1.5 text-sm">Cancelar</button>
              <button className="aero-btn aero-btn-confirm px-3 py-1.5 text-sm font-semibold">
                Registrar salida
              </button>
            </div>
          </div>
        </Panel>

        <Panel
          title="Retiro del encargado"
          hint="Mueve dinero a cuenta encargado"
          className="col-span-3"
        >
          <div className="space-y-2 text-sm">
            <Field label="Monto">
              <input className="aero-input w-full" placeholder="0.00" />
            </Field>
            <Field label="Concepto">
              <input className="aero-input w-full" placeholder="Reposición fotocopiadora" />
            </Field>
            <button className="aero-btn aero-btn-confirm w-full py-1.5 text-sm font-semibold mt-2">
              Registrar retiro
            </button>
          </div>
        </Panel>

        <Panel title="Llegada de guías" hint="Pedido / reposición" className="col-span-4">
          <div className="grid grid-cols-4 gap-2 text-xs">
            {(["Gral", "I", "II", "III"] as const).map((t) => (
              <div key={t}>
                <div className="mb-1 text-center">
                  <GuiaBadge tipo={t} />
                </div>
                <input className="aero-input w-full text-center" defaultValue="0" />
              </div>
            ))}
          </div>
          <button className="aero-btn aero-btn-confirm mt-3 w-full py-1.5 text-sm font-semibold">
            Registrar llegada
          </button>
        </Panel>
      </div>
    </AeroShell>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="block">
      <span className="mb-0.5 block text-[11px] font-semibold uppercase tracking-wide text-[oklch(0.4_0.1_250)]">
        {label}
      </span>
      {children}
    </label>
  );
}

function CajaRow({ nombre, monto, sub }: { nombre: string; monto: string; sub: string }) {
  return (
    <div className="aero-sheen rounded border border-[rgba(120,170,220,0.4)] bg-white/60 p-2">
      <div className="flex items-center justify-between">
        <span className="text-xs font-semibold text-[oklch(0.28_0.1_250)]">{nombre}</span>
        <span className="font-mono text-sm font-bold text-[oklch(0.3_0.16_245)]">{monto}</span>
      </div>
      <div className="text-[10px] text-[oklch(0.45_0.08_250)]">{sub}</div>
    </div>
  );
}
