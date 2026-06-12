import { Link, useLocation } from "@tanstack/react-router";
import type { ReactNode } from "react";

const nav = [
  { to: "/", label: "Dashboard", icon: "â–¦" },
  { to: "/venta", label: "Registrar entrega", icon: "ðŸ›’" },
  { to: "/inventario", label: "Inventario", icon: "ðŸ“¦" },
  { to: "/cuentas", label: "Cuentas (3)", icon: "ðŸ’³" },
  { to: "/cierre", label: "Cierre de caja", icon: "ðŸ§®" },
  { to: "/pedidos", label: "Pedidos", icon: "ðŸ“‘" },
];

export function AeroShell({
  title,
  subtitle,
  children,
  interactive = false,
}: {
  title: string;
  subtitle?: string;
  children: ReactNode;
  interactive?: boolean;
}) {
  const loc = useLocation();
  return (
    <div className="min-h-screen p-4 md:p-6">
      <div className="mx-auto max-w-[1400px] aero-window overflow-hidden">
        {/* Title bar */}
        <div className="aero-titlebar flex items-center justify-between px-4 py-2">
          <div className="flex items-center gap-3">
            <div className="flex h-7 w-7 items-center justify-center rounded bg-white/30 font-bold">
              CEF
            </div>
            <div className="leading-tight">
              <div className="text-sm font-semibold">
                Centro de Estudiantes de FÃ­sica Â· GuÃ­as 2026
              </div>
              <div className="text-[11px] opacity-90">
                UMSS Â· Facultad de Ciencias y TecnologÃ­a â€” Panel del encargado
              </div>
            </div>
          </div>
          <div />
        </div>

        <div className="grid grid-cols-[220px_1fr] min-h-[80vh]">
          {/* Sidebar */}
          <aside className="border-r border-[rgba(120,170,220,0.4)] bg-gradient-to-b from-[rgba(225,240,253,0.7)] to-[rgba(190,222,248,0.5)] p-3">
            <div className="mb-3 px-2 text-[10px] font-semibold uppercase tracking-wider text-[oklch(0.35_0.1_250)]">
              NavegaciÃ³n
            </div>
            <nav className="flex flex-col gap-1">
              {nav.map((n) => {
                const active = loc.pathname === n.to;
                return (
                  <Link
                    key={n.to}
                    to={n.to}
                    className={`flex items-center gap-2 rounded px-3 py-2 text-sm transition ${
                      active
                        ? "aero-btn font-semibold shadow-inner"
                        : "text-[oklch(0.28_0.08_250)] hover:bg-white/50"
                    }`}
                  >
                    <span className="text-base">{n.icon}</span>
                    {n.label}
                  </Link>
                );
              })}
            </nav>

            <div className="mt-6 px-2 text-[10px] font-semibold uppercase tracking-wider text-[oklch(0.35_0.1_250)]">
              Vista pÃºblica
            </div>
            <Link
              to="/consulta"
              className="mt-2 flex items-center gap-2 rounded px-3 py-2 text-sm text-[oklch(0.28_0.08_250)] hover:bg-white/50"
            >
              <span>ðŸ”Ž</span> Consulta estudiante
            </Link>
          </aside>

          {/* Content */}
          <main className="bg-[rgba(245,250,255,0.55)] p-5">
            <fieldset disabled={!interactive} className="contents">
              <header className="mb-4 border-b border-[rgba(120,170,220,0.5)] pb-3">
                <div>
                  <div className="text-[11px] uppercase tracking-wide text-[oklch(0.4_0.1_250)]">
                    Hoja de control
                  </div>
                  <h1 className="text-2xl font-semibold text-[oklch(0.25_0.12_250)]">{title}</h1>
                  {subtitle && <p className="text-sm text-[oklch(0.4_0.08_250)]">{subtitle}</p>}
                </div>
              </header>
              {children}
            </fieldset>
          </main>
        </div>
      </div>
      <p className="mx-auto mt-3 max-w-[1400px] text-center text-[11px] text-white/85 drop-shadow">
        UXploradores Â· InteracciÃ³n Humano-Computadora Â· I-2026
      </p>
    </div>
  );
}

export function GuiaBadge({ tipo }: { tipo: "Gral" | "I" | "II" | "III" }) {
  const cls =
    tipo === "Gral"
      ? "badge-gral"
      : tipo === "I"
        ? "badge-fis1"
        : tipo === "II"
          ? "badge-fis2"
          : "badge-fis3";
  return <span className={`aero-badge ${cls}`}>Fis {tipo}</span>;
}

export function Panel({
  title,
  hint,
  children,
  className = "",
}: {
  title: string;
  hint?: string;
  children: ReactNode;
  className?: string;
}) {
  return (
    <section className={`aero-panel overflow-hidden ${className}`}>
      <div className="aero-titlebar flex items-center justify-between px-3 py-1.5">
        <h2 className="text-sm font-semibold uppercase tracking-wide">{title}</h2>
        {hint && <span className="text-[11px] opacity-90">{hint}</span>}
      </div>
      <div className="p-3">{children}</div>
    </section>
  );
}