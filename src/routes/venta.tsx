import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useMemo, useState } from "react";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";
import { fetchGuides, registrarEntrega, type Guide } from "@/lib/data";

export const Route = createFileRoute("/venta")({
  component: VentaPage,
  head: () => ({ meta: [{ title: "Registrar entrega · CEF Guías" }] }),
});

type Metodo = "efectivo" | "qr";

function VentaPage() {
  const [guides, setGuides] = useState<Guide[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  // Estado del formulario
  const [guideId, setGuideId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [method, setMethod] = useState<Metodo | null>(null);

  // Estado del envío
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ ok: boolean; msg: string } | null>(null);

  async function loadGuides() {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await fetchGuides();
      setGuides(data);
      if (data.length && guideId === null) setGuideId(data[0].id);
    } catch (e) {
      setLoadError(e instanceof Error ? e.message : "Error al cargar guías");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadGuides();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const selected = useMemo(() => guides.find((g) => g.id === guideId) ?? null, [guides, guideId]);

  const total = selected ? selected.price * quantity : 0;
  const sinStock = selected ? quantity > selected.stock : false;
  const puedeConfirmar = !!selected && !!method && quantity > 0 && !sinStock && !saving;

  async function handleConfirm() {
    if (!selected || !method) return;
    setSaving(true);
    setFeedback(null);
    try {
      await registrarEntrega({
        guideId: selected.id,
        quantity,
        method,
      });
      setFeedback({
        ok: true,
        msg: `Entrega registrada: ${quantity} × ${selected.name} (Bs ${total.toFixed(2)})`,
      });
      // reset suave y recarga de stock
      setQuantity(1);
      setMethod(null);
      await loadGuides();
    } catch (e) {
      setFeedback({
        ok: false,
        msg: e instanceof Error ? e.message : "No se pudo registrar la entrega",
      });
    } finally {
      setSaving(false);
    }
  }

  return (
    <AeroShell
      title="Registrar entrega"
      subtitle="Asistente paso a paso — entrega de guías a estudiantes"
      interactive
    >
      <div className="grid grid-cols-12 gap-4">
        <Panel title="Nueva entrega" className="col-span-7">
          {loading ? (
            <div className="py-8 text-center text-sm text-[oklch(0.45_0.08_250)]">
              Cargando guías…
            </div>
          ) : loadError ? (
            <div className="space-y-3 py-6 text-center">
              <p className="text-sm text-[oklch(0.5_0.2_25)]">⚠ {loadError}</p>
              <button onClick={() => void loadGuides()} className="aero-btn px-4 py-2 text-sm">
                Reintentar
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              <Step n={1} label="Seleccionar guía">
                <div className="flex flex-wrap gap-2">
                  {guides.map((g) => (
                    <button
                      key={g.id}
                      onClick={() => setGuideId(g.id)}
                      className={`aero-btn px-4 py-2 text-sm ${
                        guideId === g.id
                          ? "font-semibold shadow-inner ring-2 ring-[oklch(0.5_0.18_245)]"
                          : ""
                      }`}
                    >
                      <GuiaBadge tipo={g.tipo} />
                    </button>
                  ))}
                </div>
                {selected && (
                  <p className="mt-2 text-xs text-[oklch(0.45_0.08_250)]">
                    {selected.subject} · Stock disponible:{" "}
                    <b className={selected.stock < 10 ? "text-[oklch(0.5_0.2_25)]" : ""}>
                      {selected.stock}
                    </b>
                  </p>
                )}
              </Step>

              <Step n={2} label="Cantidad">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                    className="aero-btn h-9 w-9 text-sm"
                  >
                    −
                  </button>
                  <input
                    className="aero-input w-20 text-center"
                    type="number"
                    min={1}
                    value={quantity}
                    onChange={(e) => setQuantity(Math.max(1, Number(e.target.value) || 1))}
                  />
                  <button
                    onClick={() => setQuantity((q) => q + 1)}
                    className="aero-btn h-9 w-9 text-sm"
                  >
                    +
                  </button>
                  {selected && (
                    <span className="ml-3 text-xs text-[oklch(0.45_0.08_250)]">
                      × Bs {selected.price} = <b>Bs {total.toFixed(2)}</b>
                    </span>
                  )}
                </div>
                {sinStock && (
                  <p className="mt-2 text-xs font-semibold text-[oklch(0.5_0.2_25)]">
                    ⚠ No hay stock suficiente (disponible: {selected?.stock})
                  </p>
                )}
              </Step>

              <Step n={3} label="Método de pago">
                <div className="flex gap-2">
                  <button
                    onClick={() => setMethod("efectivo")}
                    className={`aero-btn px-4 py-2 text-sm ${
                      method === "efectivo" ? "font-semibold ring-2 ring-[oklch(0.5_0.18_245)]" : ""
                    }`}
                  >
                    Efectivo → Centro
                  </button>
                  <button
                    onClick={() => setMethod("qr")}
                    className={`aero-btn px-4 py-2 text-sm ${
                      method === "qr" ? "font-semibold ring-2 ring-[oklch(0.5_0.18_245)]" : ""
                    }`}
                  >
                    QR → Banco
                  </button>
                </div>
              </Step>

              <Step n={4} label="Confirmación">
                <div className="flex items-center gap-2">
                  <button
                    onClick={() => {
                      setMethod(null);
                      setQuantity(1);
                      setFeedback(null);
                    }}
                    className="aero-btn aero-btn-danger px-4 py-2 text-sm"
                  >
                    Cancelar
                  </button>
                  <button
                    onClick={() => void handleConfirm()}
                    disabled={!puedeConfirmar}
                    className="aero-btn aero-btn-confirm px-5 py-2 text-sm font-semibold disabled:cursor-not-allowed disabled:opacity-50"
                  >
                    {saving ? "Registrando…" : "✓ Confirmar entrega"}
                  </button>
                </div>
                {feedback && (
                  <div
                    className={`mt-3 rounded px-3 py-2 text-xs font-semibold ${
                      feedback.ok
                        ? "bg-[oklch(0.92_0.08_145)] text-[oklch(0.35_0.15_145)]"
                        : "bg-[oklch(0.93_0.08_25)] text-[oklch(0.45_0.2_25)]"
                    }`}
                  >
                    {feedback.ok ? "✓ " : "⚠ "}
                    {feedback.msg}
                  </div>
                )}
              </Step>
            </div>
          )}
        </Panel>

        <Panel title="Resumen" hint="vista previa" className="col-span-5">
          <div className="space-y-1 text-sm">
            <Row k="Guía" v={selected ? <GuiaBadge tipo={selected.tipo} /> : "—"} />
            <Row k="Materia" v={selected?.subject ?? "—"} />
            <Row k="Cantidad" v={String(quantity)} />
            <Row k="Precio unitario" v={selected ? `Bs ${selected.price.toFixed(2)}` : "—"} />
            <Row k="Método" v={method === "efectivo" ? "Efectivo" : method === "qr" ? "QR" : "—"} />
            <Row
              k="Cuenta destino"
              v={method === "efectivo" ? "Caja del Centro" : method === "qr" ? "Banco" : "—"}
            />
            <div className="mt-3 flex justify-between border-t border-[rgba(120,170,220,0.4)] pt-2">
              <span className="font-semibold">Total</span>
              <span className="font-mono font-bold text-[oklch(0.3_0.16_245)]">
                Bs {total.toFixed(2)}
              </span>
            </div>
          </div>
          {!method && selected && (
            <p className="mt-3 text-[11px] text-[oklch(0.45_0.08_250)]">
              Selecciona un método de pago para poder confirmar.
            </p>
          )}
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
