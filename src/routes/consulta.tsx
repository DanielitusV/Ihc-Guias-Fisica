import { createFileRoute, Link } from "@tanstack/react-router";

export const Route = createFileRoute("/consulta")({
  component: ConsultaPage,
  head: () => ({ meta: [{ title: "Consulta de guías · Centro de Estudiantes de Física" }] }),
});

const guias = [
  {
    tipo: "Gral",
    materia: "Física General",
    carreras: "Civil, Industrial, Química, Alimentos, Mecánica, Agroindustrial",
    stock: 6,
    precio: 35,
    badge: "badge-gral",
  },
  {
    tipo: "I",
    materia: "Física Básica I",
    carreras: "Informática, Sistemas, Electrónica, Eléctrica, Lic. Física, Lic. Matemática",
    stock: 5,
    precio: 35,
    badge: "badge-fis1",
  },
  {
    tipo: "II",
    materia: "Física Básica II",
    carreras: "Informática, Sistemas, Electrónica, Eléctrica, Lic. Física, Lic. Matemática",
    stock: 44,
    precio: 35,
    badge: "badge-fis2",
  },
  {
    tipo: "III",
    materia: "Física Básica III",
    carreras: "Electrónica, Eléctrica, Lic. Física",
    stock: 50,
    precio: 35,
    badge: "badge-fis3",
  },
];

const horarios = [
  { dia: "Lunes", hora: "10:00 – 14:00", abierto: true },
  { dia: "Martes", hora: "10:00 – 14:00", abierto: true },
  { dia: "Miércoles", hora: "10:00 – 14:00", abierto: true },
  { dia: "Jueves", hora: "10:00 – 14:00", abierto: true },
  { dia: "Viernes", hora: "10:00 – 14:00", abierto: true },
  { dia: "Sábado", hora: "10:00 – 12:30", abierto: true },
  { dia: "Domingo", hora: "Cerrado", abierto: false },
];

function ConsultaPage() {
  return (
    <div className="min-h-screen p-4 md:p-6">
      <div className="mx-auto max-w-6xl aero-window overflow-hidden">
        <div className="aero-titlebar px-5 py-2.5">
          <div className="text-sm font-semibold">Consulta pública — Guías de Física · CEF UMSS</div>
        </div>

        <div className="bg-[rgba(245,250,255,0.6)] px-8 py-6">
          {/* Cabecera */}
          <header className="flex flex-wrap items-end justify-between gap-6 pb-5">
            <div className="space-y-1">
              <div className="text-[11px] uppercase tracking-[0.18em] text-[oklch(0.45_0.1_250)]">
                Antes de venir al Centro
              </div>
              <h1 className="text-2xl font-bold leading-tight text-[oklch(0.25_0.12_250)]">
                Disponibilidad de guías de laboratorio
              </h1>
              <p className="text-xs text-[oklch(0.45_0.08_250)]">
                Revisa el stock e identifica qué guía corresponde a tu materia.
              </p>
            </div>
            <div className="aero-panel px-4 py-2.5 text-xs leading-relaxed">
              <div className="text-[10px] font-semibold uppercase tracking-wider text-[oklch(0.45_0.1_250)]">
                Ubicación
              </div>
              <div className="font-semibold text-[oklch(0.3_0.12_250)]">
                Centro de Estudiantes de Física
              </div>
              <div className="text-[oklch(0.45_0.08_250)]">
                FCyT · UMSS — Frente al aula 617, primer piso
              </div>
            </div>
          </header>

          {/* GRUPO 1 — Guías (proximidad + similitud) */}
          <section className="border-t border-[rgba(120,170,220,0.45)] pt-5">
            <div className="mb-3 flex items-baseline justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[oklch(0.3_0.1_250)]">
                ¿Qué guía me toca?
              </h2>
              <span className="text-[11px] text-[oklch(0.45_0.08_250)]">
                Identifica tu guía por carrera
              </span>
            </div>

            <div className="grid grid-cols-2 gap-5 xl:grid-cols-4">
              {guias.map((g) => (
                <article key={g.tipo} className="aero-panel flex flex-col p-4">
                  <div className="flex items-center justify-between">
                    <span className={`aero-badge ${g.badge}`}>Fis {g.tipo}</span>
                    <span className="font-mono text-sm font-bold text-[oklch(0.3_0.16_245)]">
                      Bs {g.precio}
                    </span>
                  </div>

                  <div className="mt-3 text-sm font-semibold text-[oklch(0.25_0.12_250)]">
                    {g.materia}
                  </div>
                  <div className="mt-1 text-xs">
                    {g.stock > 10 ? (
                      <span className="font-semibold text-[oklch(0.4_0.15_145)]">
                        ✓ Disponible ({g.stock} en stock)
                      </span>
                    ) : g.stock > 0 ? (
                      <span className="font-semibold text-[oklch(0.5_0.18_60)]">
                        ⚠ Quedan pocas ({g.stock})
                      </span>
                    ) : (
                      <span className="font-semibold text-[oklch(0.5_0.2_25)]">✗ Sin stock</span>
                    )}
                  </div>

                  <div className="mt-4 border-t border-[rgba(120,170,220,0.35)] pt-3 text-[11px] leading-relaxed text-[oklch(0.4_0.08_250)]">
                    <div className="mb-1 text-[10px] font-semibold uppercase tracking-wider text-[oklch(0.45_0.1_250)]">
                      Carreras que la cursan
                    </div>
                    {g.carreras}
                  </div>
                </article>
              ))}
            </div>
          </section>

          {/* GRUPO 2 — Información logística (proximidad) */}
          <section className="mt-7 border-t border-[rgba(120,170,220,0.45)] pt-5">
            <div className="mb-3 flex items-baseline justify-between">
              <h2 className="text-sm font-semibold uppercase tracking-[0.14em] text-[oklch(0.3_0.1_250)]">
                Información de atención
              </h2>
              <span className="text-[11px] text-[oklch(0.45_0.08_250)]">
                Cuándo venir y cómo pagar
              </span>
            </div>

            <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
              <div className="aero-panel overflow-hidden md:col-span-2">
                <div className="aero-titlebar px-4 py-1.5">
                  <h3 className="text-sm font-semibold uppercase tracking-wide">
                    Horarios de atención
                  </h3>
                </div>
                <table className="w-full text-xs">
                  <tbody>
                    {horarios.map((h) => (
                      <tr
                        key={h.dia}
                        className="border-b border-[rgba(120,170,220,0.3)] last:border-0"
                      >
                        <td className="px-4 py-2 font-semibold text-[oklch(0.3_0.1_250)]">
                          {h.dia}
                        </td>
                        <td
                          className={`px-4 py-2 text-right font-mono ${
                            h.abierto ? "text-[oklch(0.3_0.12_250)]" : "text-[oklch(0.5_0.2_25)]"
                          }`}
                        >
                          {h.hora}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>

              <div className="aero-panel overflow-hidden">
                <div className="aero-titlebar px-4 py-1.5">
                  <h3 className="text-sm font-semibold uppercase tracking-wide">Formas de pago</h3>
                </div>
                <ul className="space-y-3 p-4 text-xs leading-relaxed text-[oklch(0.3_0.1_250)]">
                  <li>
                    <div className="font-semibold text-[oklch(0.25_0.12_250)]">Efectivo</div>
                    <div className="text-[oklch(0.45_0.08_250)]">Preferible monto exacto.</div>
                  </li>
                  <li>
                    <div className="font-semibold text-[oklch(0.25_0.12_250)]">QR</div>
                    <div className="text-[oklch(0.45_0.08_250)]">
                      Cualquier banco. Muestra el comprobante al encargado.
                    </div>
                  </li>
                </ul>
              </div>
            </div>
          </section>

          <footer className="mt-7 flex items-center justify-between border-t border-[rgba(120,170,220,0.45)] pt-4 text-[11px] text-[oklch(0.45_0.08_250)]">
            <span>Centro de Estudiantes de Física · CEF UMSS</span>
            <Link to="/" className="aero-btn px-4 py-1.5 text-xs">
              ← Volver al panel
            </Link>
          </footer>
        </div>
      </div>
    </div>
  );
}
