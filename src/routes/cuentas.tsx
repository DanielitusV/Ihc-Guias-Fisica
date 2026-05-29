import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { AeroShell, Panel } from "@/components/aero-shell";

export const Route = createFileRoute("/cuentas")({
  component: CuentasPage,
  head: () => ({ meta: [{ title: "Cuentas · CEF Guías" }] }),
});

type Cuenta = "centro" | "banco" | "encargado";

const data = {
  centro: {
    nombre: "Caja del Centro",
    sub: "Dinero físico en el centro",
    saldo: "Bs 231.50",
    movs: [
      { fecha: "26/05", concepto: "Venta Fis I (efectivo)", monto: "+35.00" },
      { fecha: "26/05", concepto: "Venta Fis II (efectivo)", monto: "+35.00" },
      { fecha: "26/05", concepto: "Retiro encargado", monto: "−500.00" },
      { fecha: "25/05", concepto: "Pago agua", monto: "−45.00" },
    ],
  },
  banco: {
    nombre: "Cuenta Banco (QR — Soto)",
    sub: "Pagos por QR de estudiantes",
    saldo: "Bs 9 905.00",
    movs: [
      { fecha: "26/05", concepto: "Venta Fis I (QR)", monto: "+35.00" },
      { fecha: "26/05", concepto: "Venta Fis Gral (QR)", monto: "+35.00" },
      { fecha: "25/05", concepto: "Pago internet", monto: "−120.00" },
      { fecha: "24/05", concepto: "Compra QR (mandado)", monto: "−80.00" },
    ],
  },
  encargado: {
    nombre: "Cuenta Encargado",
    sub: "Dinero retirado del centro para pagar a fotocopiadora",
    saldo: "Bs 1 240.00",
    movs: [
      { fecha: "26/05", concepto: "Retiro desde centro", monto: "+500.00" },
      { fecha: "20/05", concepto: "Pago fotocopiadora (deuda −1 200)", monto: "−1 200.00" },
      { fecha: "18/05", concepto: "Repuesto impresora", monto: "−85.00" },
    ],
  },
};

function CuentasPage() {
  const [tab, setTab] = useState<Cuenta>("centro");
  const c = data[tab];

  return (
    <AeroShell
      title="Cuentas (3)"
      subtitle="Cada movimiento se registra en una sola cuenta. Conversiones físico↔QR mueven dinero entre cuentas."
    >
      <div className="mb-3 flex gap-1">
        {(Object.keys(data) as Cuenta[]).map((k) => (
          <button
            key={k}
            onClick={() => setTab(k)}
            className={`aero-btn px-4 py-2 text-sm ${tab === k ? "font-semibold shadow-inner" : ""}`}
          >
            {data[k].nombre}
          </button>
        ))}
      </div>

      <div className="grid grid-cols-12 gap-4">
        <Panel title={c.nombre} hint={c.sub} className="col-span-8">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Concepto</th>
                <th className="text-right">Monto (Bs)</th>
              </tr>
            </thead>
            <tbody>
              {c.movs.map((m, i) => (
                <tr key={i}>
                  <td className="font-mono text-xs">{m.fecha}</td>
                  <td>{m.concepto}</td>
                  <td
                    className={`text-right font-mono font-semibold ${
                      m.monto.startsWith("+")
                        ? "text-[oklch(0.45_0.15_145)]"
                        : "text-[oklch(0.5_0.2_25)]"
                    }`}
                  >
                    {m.monto}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        <div className="col-span-4 space-y-4">
          <Panel title="Saldo actual">
            <div className="text-center py-2">
              <div className="text-[11px] uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Disponible
              </div>
              <div className="font-mono text-3xl font-bold text-[oklch(0.3_0.16_245)]">
                {c.saldo}
              </div>
            </div>
          </Panel>

          <Panel title="Acciones">
            <div className="space-y-2">
              <button className="aero-btn aero-btn-confirm w-full py-2 text-sm font-semibold">
                + Registrar entrada
              </button>
              <button className="aero-btn w-full py-2 text-sm">− Registrar salida</button>
              <button className="aero-btn w-full py-2 text-sm">⇄ Conversión Físico ↔ QR</button>
              {tab === "encargado" && (
                <button className="aero-btn aero-btn-confirm w-full py-2 text-sm font-semibold">
                  Pagar deuda fotocopiadora
                </button>
              )}
            </div>
          </Panel>

          {tab === "encargado" && (
            <Panel title="Deuda fotocopiadora">
              <div className="text-center py-2">
                <div className="text-[11px] uppercase tracking-wide">Pendiente</div>
                <div className="font-mono text-2xl font-bold text-[oklch(0.5_0.2_25)]">
                  Bs 27 243.68
                </div>
                <p className="mt-1 text-[11px] text-[oklch(0.45_0.08_250)]">
                  Al pagar disminuye automáticamente.
                </p>
              </div>
            </Panel>
          )}
        </div>
      </div>
    </AeroShell>
  );
}
