import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";

export const Route = createFileRoute("/inventario")({
  component: InventarioPage,
  head: () => ({ meta: [{ title: "Inventario · CEF Guías" }] }),
});

const rows = [
  {
    tipo: "Gral" as const,
    fiadas: 50,
    compradas: 200,
    efectivo: 180,
    qr: 64,
    stock: 6,
    estado: "Reponer",
  },
  {
    tipo: "I" as const,
    fiadas: 30,
    compradas: 150,
    efectivo: 120,
    qr: 55,
    stock: 5,
    estado: "Reponer",
  },
  {
    tipo: "II" as const,
    fiadas: 20,
    compradas: 120,
    efectivo: 60,
    qr: 36,
    stock: 44,
    estado: "OK",
  },
  {
    tipo: "III" as const,
    fiadas: 10,
    compradas: 100,
    efectivo: 30,
    qr: 20,
    stock: 50,
    estado: "OK",
  },
];

function InventarioPage() {
  return (
    <AeroShell title="Inventario" subtitle="Stock real por guía — incluye fiadas y compradas">
      <Panel title="Detalle de inventario por guía">
        <table className="aero-table">
          <thead>
            <tr>
              <th>Guía</th>
              <th>Fiadas</th>
              <th>Compradas</th>
              <th>Vendidas efectivo</th>
              <th>Vendidas QR</th>
              <th>Stock restante</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr key={r.tipo}>
                <td>
                  <GuiaBadge tipo={r.tipo} />
                </td>
                <td>{r.fiadas}</td>
                <td>{r.compradas}</td>
                <td>{r.efectivo}</td>
                <td>{r.qr}</td>
                <td className={`font-bold ${r.stock < 10 ? "text-[oklch(0.5_0.2_25)]" : ""}`}>
                  {r.stock}
                </td>
                <td>
                  <span className={`aero-badge ${r.estado === "OK" ? "badge-gral" : "badge-fis3"}`}>
                    {r.estado}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
        <p className="mt-3 text-[11px] text-[oklch(0.45_0.08_250)]">
          Stock = Fiadas + Compradas − (Vendidas efectivo + Vendidas QR). En rojo: stock bajo.
        </p>
      </Panel>
    </AeroShell>
  );
}
