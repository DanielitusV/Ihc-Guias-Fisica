import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { AeroShell, Panel } from "@/components/aero-shell";
import { getDashboardData, type DashboardData } from "@/lib/dashboard";

export const Route = createFileRoute("/")({
  component: Dashboard,
  head: () => ({ meta: [{ title: "Dashboard - CEF Guias" }] }),
});

const currency = new Intl.NumberFormat("es-BO", {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

function Dashboard() {
  const [data, setData] = useState<DashboardData | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [error, setError] = useState("");

  async function loadDashboard() {
    setStatus("loading");
    setError("");

    try {
      const dashboardData = await getDashboardData();
      setData(dashboardData);
      setStatus("ready");
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : "No se pudo cargar el dashboard");
      setStatus("error");
    }
  }

  useEffect(() => {
    void loadDashboard();
  }, []);

  const totalStock = useMemo(
    () => data?.guides.reduce((total, guide) => total + guide.stock, 0) ?? 0,
    [data],
  );

  const lowStock = useMemo(() => data?.guides.filter((guide) => guide.stock <= 10) ?? [], [data]);

  const totalBalance = useMemo(
    () => data?.accounts.reduce((total, account) => total + account.balance, 0) ?? 0,
    [data],
  );

  return (
    <AeroShell
      title="Dashboard"
      subtitle="Resumen operativo conectado a Supabase"
      lockContent={false}
    >
      <div className="mb-4 flex items-center justify-between gap-3 rounded border border-[rgba(120,170,220,0.45)] bg-white/65 px-3 py-2 text-sm">
        <div>
          <div className="font-semibold text-[oklch(0.28_0.1_250)]">
            Estado: {status === "loading" ? "cargando" : status === "error" ? "error" : "listo"}
          </div>
          <div className="text-xs text-[oklch(0.42_0.08_250)]">
            {status === "error"
              ? error
              : "Datos reales desde las tablas guides, accounts y movimientos."}
          </div>
        </div>
        <button className="aero-btn px-3 py-1.5 text-sm font-semibold" onClick={loadDashboard}>
          Actualizar
        </button>
      </div>

      {status === "loading" && <LoadingState />}
      {status === "error" && <ErrorState message={error} onRetry={loadDashboard} />}
      {status === "ready" && data && (
        <div className="grid grid-cols-12 gap-4">
          <SummaryCard
            label="Guias en stock"
            value={totalStock.toString()}
            detail="Unidades totales"
          />
          <SummaryCard
            label="Stock bajo"
            value={lowStock.length.toString()}
            detail="Guias con 10 o menos"
            danger={lowStock.length > 0}
          />
          <SummaryCard
            label="Saldo registrado"
            value={`Bs ${currency.format(totalBalance)}`}
            detail="Cuentas del prototipo"
          />

          <Panel title="Stock de guias" hint="Tabla guides" className="col-span-7">
            {data.guides.length === 0 ? (
              <EmptyState text="No hay guias cargadas. Ejecuta supabase/seed.sql para datos de prueba." />
            ) : (
              <table className="aero-table">
                <thead>
                  <tr>
                    <th>Guia</th>
                    <th>Materia</th>
                    <th>Precio</th>
                    <th>Stock</th>
                    <th>Estado</th>
                  </tr>
                </thead>
                <tbody>
                  {data.guides.map((guide) => (
                    <tr key={guide.id}>
                      <td className="font-semibold">{guide.name}</td>
                      <td>{guide.subject}</td>
                      <td>Bs {currency.format(guide.price)}</td>
                      <td
                        className={
                          guide.stock <= 10 ? "font-bold text-[oklch(0.5_0.2_25)]" : "font-semibold"
                        }
                      >
                        {guide.stock}
                      </td>
                      <td>
                        <span className={guide.stock <= 10 ? "status-danger" : "status-ok"}>
                          {guide.stock <= 10 ? "Reponer" : "Disponible"}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Panel>

          <Panel title="Resumen de cuentas" hint="Saldos calculados" className="col-span-5">
            {data.accounts.length === 0 ? (
              <EmptyState text="No hay cuentas cargadas." />
            ) : (
              <div className="space-y-2">
                {data.accounts.map((account) => (
                  <div
                    className="aero-sheen rounded border border-[rgba(120,170,220,0.4)] bg-white/60 p-2"
                    key={account.id}
                  >
                    <div className="flex items-center justify-between gap-3">
                      <span className="text-xs font-semibold text-[oklch(0.28_0.1_250)]">
                        {account.name}
                      </span>
                      <span className="font-mono text-sm font-bold text-[oklch(0.3_0.16_245)]">
                        Bs {currency.format(account.balance)}
                      </span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </Panel>

          <Panel title="Movimientos recientes" hint="Inventario y cuentas" className="col-span-12">
            {data.recentMovements.length === 0 ? (
              <EmptyState text="Todavia no hay movimientos registrados." />
            ) : (
              <table className="aero-table">
                <thead>
                  <tr>
                    <th>Fecha</th>
                    <th>Origen</th>
                    <th>Registro</th>
                    <th>Detalle</th>
                  </tr>
                </thead>
                <tbody>
                  {data.recentMovements.map((movement) => (
                    <tr key={movement.id}>
                      <td className="font-mono text-xs">{formatDate(movement.date)}</td>
                      <td>{movement.source}</td>
                      <td className="font-semibold">{movement.title}</td>
                      <td>{movement.detail}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Panel>
        </div>
      )}
    </AeroShell>
  );
}

function SummaryCard({
  label,
  value,
  detail,
  danger = false,
}: {
  label: string;
  value: string;
  detail: string;
  danger?: boolean;
}) {
  return (
    <Panel title={label} className="col-span-4">
      <div className={danger ? "text-[oklch(0.5_0.2_25)]" : "text-[oklch(0.3_0.16_245)]"}>
        <div className="text-3xl font-bold">{value}</div>
        <div className="text-xs font-semibold uppercase tracking-wide">{detail}</div>
      </div>
    </Panel>
  );
}

function LoadingState() {
  return (
    <div className="rounded border border-[rgba(120,170,220,0.45)] bg-white/65 p-4 text-sm font-semibold">
      Cargando datos del dashboard...
    </div>
  );
}

function ErrorState({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="rounded border border-[rgba(220,80,80,0.45)] bg-white/75 p-4 text-sm">
      <div className="font-bold text-[oklch(0.5_0.2_25)]">No se pudo cargar Supabase.</div>
      <div className="mt-1">{message}</div>
      <button className="aero-btn mt-3 px-3 py-1.5 font-semibold" onClick={onRetry}>
        Reintentar
      </button>
    </div>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <div className="rounded border border-dashed border-[rgba(120,170,220,0.5)] bg-white/55 p-4 text-center text-sm font-semibold text-[oklch(0.42_0.08_250)]">
      {text}
    </div>
  );
}

function formatDate(value: string) {
  return new Intl.DateTimeFormat("es-BO", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}
