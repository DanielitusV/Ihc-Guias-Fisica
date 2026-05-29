import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";

export const Route = createFileRoute("/venta")({
  component: VentaPage,
  head: () => ({ meta: [{ title: "Registrar venta · CEF Guías" }] }),
});

function VentaPage() {
  return (
    <AeroShell title="Registrar venta" subtitle="Asistente paso a paso — para ventas con detalle">
      <div className="grid grid-cols-12 gap-4">
        <Panel title="Nueva venta" className="col-span-7">
          <div className="space-y-4">
            <Step n={1} label="Seleccionar guía">
              <div className="flex flex-wrap gap-2">
                {(["Gral", "I", "II", "III"] as const).map((t) => (
                  <button key={t} className="aero-btn px-4 py-2 text-sm">
                    <GuiaBadge tipo={t} />
                  </button>
                ))}
              </div>
            </Step>
            <Step n={2} label="Cantidad">
              <input className="aero-input w-24 text-center" defaultValue={1} />
              <span className="ml-3 text-xs text-[oklch(0.45_0.08_250)]">
                × Bs 35 = <b>Bs 35</b>
              </span>
            </Step>
            <Step n={3} label="Método de pago">
              <div className="flex gap-2">
                <button className="aero-btn px-4 py-2 text-sm">Efectivo → Centro</button>
                <button className="aero-btn px-4 py-2 text-sm">QR → Banco</button>
              </div>
            </Step>
            <Step n={4} label="Confirmación">
              <div className="flex gap-2">
                <button className="aero-btn aero-btn-danger px-4 py-2 text-sm">Cancelar</button>
                <button className="aero-btn aero-btn-confirm px-5 py-2 text-sm font-semibold">
                  ✓ Confirmar venta
                </button>
              </div>
            </Step>
          </div>
        </Panel>

        <Panel title="Resumen" hint="vista previa" className="col-span-5">
          <div className="space-y-1 text-sm">
            <Row k="Guía" v={<GuiaBadge tipo="I" />} />
            <Row k="Cantidad" v="1" />
            <Row k="Precio unitario" v="Bs 35.00" />
            <Row k="Método" v="Efectivo" />
            <Row k="Cuenta destino" v="Caja del Centro" />
            <div className="mt-3 border-t border-[rgba(120,170,220,0.4)] pt-2 flex justify-between">
              <span className="font-semibold">Total</span>
              <span className="font-mono font-bold text-[oklch(0.3_0.16_245)]">Bs 35.00</span>
            </div>
          </div>
        </Panel>
      </div>
    </AeroShell>
  );
}

function Step({ n, label, children }: { n: number; label: string; children: React.ReactNode }) {
  return (
    <div className="aero-panel p-3">
      <div className="mb-2 flex items-center gap-2">
        <span className="flex h-6 w-6 items-center justify-center rounded-full bg-[oklch(0.5_0.18_245)] text-xs font-bold text-white shadow">
          {n}
        </span>
        <span className="text-sm font-semibold text-[oklch(0.25_0.12_250)]">{label}</span>
      </div>
      <div className="pl-8">{children}</div>
    </div>
  );
}

function Row({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between border-b border-[rgba(120,170,220,0.25)] py-1">
      <span className="text-xs uppercase tracking-wide text-[oklch(0.45_0.08_250)]">{k}</span>
      <span>{v}</span>
    </div>
  );
}
