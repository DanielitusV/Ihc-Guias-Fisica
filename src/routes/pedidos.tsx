import { createFileRoute } from "@tanstack/react-router";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";

export const Route = createFileRoute("/pedidos")({
  component: PedidosPage,
  head: () => ({ meta: [{ title: "Pedidos · CEF Guías" }] }),
});

const pedidos = [
  {
    fecha: "20/05/26",
    tipo: "Gral" as const,
    cant: 50,
    precio: 18,
    total: 900,
    pagado: false,
    com: "Entregado parcial",
  },
  {
    fecha: "15/05/26",
    tipo: "II" as const,
    cant: 40,
    precio: 22,
    total: 880,
    pagado: true,
    com: "OK",
  },
  {
    fecha: "10/05/26",
    tipo: "III" as const,
    cant: 30,
    precio: 25,
    total: 750,
    pagado: false,
    com: "Pendiente revisión",
  },
];

function PedidosPage() {
  return (
    <AeroShell
      title="Pedidos a fotocopiadora"
      subtitle="Llegadas de guías y deuda con el proveedor"
    >
      <div className="grid grid-cols-12 gap-4">
        <Panel title="Historial de pedidos" className="col-span-8">
          <table className="aero-table">
            <thead>
              <tr>
                <th>Fecha</th>
                <th>Guía</th>
                <th>Cant.</th>
                <th>Precio U.</th>
                <th>Total</th>
                <th>Estado</th>
                <th>Comentario</th>
              </tr>
            </thead>
            <tbody>
              {pedidos.map((p, i) => (
                <tr key={i}>
                  <td className="font-mono text-xs">{p.fecha}</td>
                  <td>
                    <GuiaBadge tipo={p.tipo} />
                  </td>
                  <td>{p.cant}</td>
                  <td className="font-mono">Bs {p.precio}</td>
                  <td className="font-mono font-semibold">Bs {p.total}</td>
                  <td>
                    <span className={`aero-badge ${p.pagado ? "badge-gral" : "badge-fis3"}`}>
                      {p.pagado ? "Pagado" : "Pendiente"}
                    </span>
                  </td>
                  <td className="text-xs">{p.com}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </Panel>

        <Panel title="Registrar llegada" className="col-span-4">
          <div className="space-y-2 text-sm">
            <label className="block">
              <span className="text-[11px] uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Fecha
              </span>
              <input type="date" className="aero-input w-full" defaultValue="2026-05-26" />
            </label>
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
            <label className="block">
              <span className="text-[11px] uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Comentario
              </span>
              <input className="aero-input w-full" placeholder="Ej: guía incorrecta, reposición…" />
            </label>
            <div className="flex gap-2 pt-1">
              <button className="aero-btn aero-btn-danger flex-1 py-1.5 text-sm">Cancelar</button>
              <button className="aero-btn aero-btn-confirm flex-1 py-1.5 text-sm font-semibold">
                Registrar
              </button>
            </div>
          </div>
        </Panel>
      </div>
    </AeroShell>
  );
}
