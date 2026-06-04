import { createFileRoute } from "@tanstack/react-router";
import { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";
import { supabase } from "@/lib/supabase";

export const Route = createFileRoute("/pedidos")({
  component: PedidosPage,
  head: () => ({ meta: [{ title: "Pedidos · CEF Guías" }] }),
});

type GuiaTipo = "Gral" | "I" | "II" | "III";

type OrderRecord = {
  id: number;
  supplier: string;
  status: string;
  total_cost: number | string | null;
  created_at: string;
};

type GuideRecord = {
  id: number;
  name: string;
  subject: string;
  price: number | string | null;
  stock: number | null;
};

type PedidoRow = {
  id: number;
  fecha: string;
  tipo: GuiaTipo;
  cant: number;
  precio: number;
  total: number;
  pagado: boolean;
  com: string;
};

type QuantityState = Record<GuiaTipo, string>;

const guideTypes = ["Gral", "I", "II", "III"] as const;
const defaultArrivalDate = "2026-05-26";

function emptyQuantities(): QuantityState {
  return {
    Gral: "0",
    I: "0",
    II: "0",
    III: "0",
  };
}

function normalizeText(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toUpperCase();
}

function getGuideTipo(guide: Pick<GuideRecord, "name" | "subject">): GuiaTipo {
  const text = normalizeText(`${guide.name} ${guide.subject}`);

  if (/\b(GRAL|GENERAL)\b/.test(text)) return "Gral";
  if (/\b(III|3)\b/.test(text)) return "III";
  if (/\b(II|2)\b/.test(text)) return "II";
  if (/\b(I|1)\b/.test(text)) return "I";

  return "Gral";
}

function inferGuideTipoFromOrder(order: OrderRecord): GuiaTipo {
  const text = normalizeText(order.supplier);

  if (/\b(GRAL|GENERAL)\b/.test(text)) return "Gral";
  if (/\b(III|3)\b/.test(text)) return "III";
  if (/\b(II|2)\b/.test(text)) return "II";
  if (/\b(I|1)\b/.test(text)) return "I";

  return "Gral";
}

function toNumber(value: number | string | null | undefined) {
  return Number(value) || 0;
}

function formatOrderDate(value: string) {
  return new Intl.DateTimeFormat("es-BO", {
    day: "2-digit",
    month: "2-digit",
    year: "2-digit",
  }).format(new Date(value));
}

function toPedidoRow(order: OrderRecord): PedidoRow {
  const total = toNumber(order.total_cost);

  return {
    id: order.id,
    fecha: formatOrderDate(order.created_at),
    tipo: inferGuideTipoFromOrder(order),
    cant: 0,
    precio: 0,
    total,
    pagado: normalizeText(order.status).includes("PAGADO"),
    com: order.supplier,
  };
}

function PedidosPage() {
  const pageRef = useRef<HTMLDivElement>(null);
  const [pedidos, setPedidos] = useState<PedidoRow[]>([]);
  const [guides, setGuides] = useState<GuideRecord[]>([]);
  const [fecha, setFecha] = useState(defaultArrivalDate);
  const [cantidades, setCantidades] = useState<QuantityState>(() => emptyQuantities());
  const [comentario, setComentario] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useLayoutEffect(() => {
    const fieldset = pageRef.current?.closest("fieldset");

    if (!(fieldset instanceof HTMLFieldSetElement)) return;

    const wasDisabled = fieldset.disabled;
    fieldset.disabled = false;

    return () => {
      fieldset.disabled = wasDisabled;
    };
  }, []);

  const loadPedidos = useCallback(async () => {
    setLoading(true);
    setError(null);

    const [ordersResponse, guidesResponse] = await Promise.all([
      supabase
        .from("orders")
        .select("id, supplier, status, total_cost, created_at")
        .order("created_at", { ascending: false }),
      supabase
        .from("guides")
        .select("id, name, subject, price, stock")
        .order("id", { ascending: true }),
    ]);

    if (ordersResponse.error) {
      setPedidos([]);
      setError(ordersResponse.error.message);
      setLoading(false);
      return;
    }

    if (guidesResponse.error) {
      setGuides([]);
      setError(guidesResponse.error.message);
      setLoading(false);
      return;
    }

    setPedidos(((ordersResponse.data ?? []) as OrderRecord[]).map(toPedidoRow));
    setGuides((guidesResponse.data ?? []) as GuideRecord[]);
    setLoading(false);
  }, []);

  useEffect(() => {
    void loadPedidos();
  }, [loadPedidos]);

  function handleQuantityChange(tipo: GuiaTipo, value: string) {
    setCantidades((current) => ({
      ...current,
      [tipo]: value.replace(/[^\d]/g, ""),
    }));
  }

  function resetForm() {
    setFecha(defaultArrivalDate);
    setCantidades(emptyQuantities());
    setComentario("");
    setFormError(null);
  }

  async function updateGuideStock(guideId: number, quantity: number) {
    const { data, error: currentStockError } = await supabase
      .from("guides")
      .select("stock")
      .eq("id", guideId)
      .single();

    if (currentStockError) throw currentStockError;

    const nextStock = toNumber(data?.stock) + quantity;
    const { error: updateError } = await supabase
      .from("guides")
      .update({ stock: nextStock })
      .eq("id", guideId);

    if (updateError) throw updateError;
  }

  async function handleRegister() {
    setFormError(null);

    const arrivals = guideTypes
      .map((tipo) => ({
        tipo,
        quantity: Number(cantidades[tipo]) || 0,
      }))
      .filter((arrival) => arrival.quantity > 0);

    if (!fecha) {
      setFormError("Selecciona una fecha.");
      return;
    }

    if (arrivals.length === 0) {
      setFormError("Ingresa al menos una cantidad.");
      return;
    }

    const guidesByTipo = new Map(guides.map((guide) => [getGuideTipo(guide), guide]));
    const missingGuide = arrivals.find((arrival) => !guidesByTipo.has(arrival.tipo));

    if (missingGuide) {
      setFormError(`No existe una guía ${missingGuide.tipo} registrada en Supabase.`);
      return;
    }

    setSaving(true);

    try {
      const createdAt = new Date(`${fecha}T12:00:00`).toISOString();
      const supplier = comentario.trim() || "Fotocopiadora";
      const totalCost = arrivals.reduce((total, arrival) => {
        const guide = guidesByTipo.get(arrival.tipo);
        return total + toNumber(guide?.price) * arrival.quantity;
      }, 0);

      const { data: order, error: orderError } = await supabase
        .from("orders")
        .insert({
          supplier,
          status: "pendiente",
          total_cost: totalCost,
          created_at: createdAt,
        })
        .select("id")
        .single();

      if (orderError) throw orderError;

      const note = `Pedido #${order.id}${comentario.trim() ? `: ${comentario.trim()}` : ""}`;

      const movements = arrivals.map((arrival) => {
        const guide = guidesByTipo.get(arrival.tipo);

        if (!guide) {
          throw new Error(`No existe una guía ${arrival.tipo} registrada en Supabase.`);
        }

        return {
          guide_id: guide.id,
          type: "entrada",
          quantity: arrival.quantity,
          note,
          created_at: createdAt,
        };
      });

      const { error: movementsError } = await supabase
        .from("inventory_movements")
        .insert(movements);

      if (movementsError) throw movementsError;

      for (const arrival of arrivals) {
        const guide = guidesByTipo.get(arrival.tipo);

        if (guide) {
          await updateGuideStock(guide.id, arrival.quantity);
        }
      }

      resetForm();
      await loadPedidos();
    } catch (requestError) {
      setFormError(requestError instanceof Error ? requestError.message : "No se pudo registrar.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <AeroShell
      title="Pedidos a fotocopiadora"
      subtitle="Llegadas de guías y deuda con el proveedor"
    >
      <div ref={pageRef} className="grid grid-cols-12 gap-4">
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
              {loading && (
                <tr>
                  <td colSpan={7} className="text-center text-xs">
                    Cargando pedidos...
                  </td>
                </tr>
              )}
              {!loading && error && (
                <tr>
                  <td colSpan={7} className="text-center text-xs text-[oklch(0.5_0.2_25)]">
                    No se pudo cargar pedidos: {error}
                  </td>
                </tr>
              )}
              {!loading && !error && pedidos.length === 0 && (
                <tr>
                  <td colSpan={7} className="text-center text-xs">
                    Sin pedidos registrados.
                  </td>
                </tr>
              )}
              {pedidos.map((p) => (
                <tr key={p.id}>
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
              <input
                type="date"
                className="aero-input w-full"
                value={fecha}
                onChange={(event) => setFecha(event.target.value)}
              />
            </label>
            <div className="grid grid-cols-4 gap-2 text-xs">
              {guideTypes.map((t) => (
                <div key={t}>
                  <div className="mb-1 text-center">
                    <GuiaBadge tipo={t} />
                  </div>
                  <input
                    className="aero-input w-full text-center"
                    inputMode="numeric"
                    value={cantidades[t]}
                    onChange={(event) => handleQuantityChange(t, event.target.value)}
                  />
                </div>
              ))}
            </div>
            <label className="block">
              <span className="text-[11px] uppercase tracking-wide text-[oklch(0.45_0.08_250)]">
                Comentario
              </span>
              <input
                className="aero-input w-full"
                placeholder="Ej: guía incorrecta, reposición…"
                value={comentario}
                onChange={(event) => setComentario(event.target.value)}
              />
            </label>
            {formError && <p className="text-xs text-[oklch(0.5_0.2_25)]">{formError}</p>}
            <div className="flex gap-2 pt-1">
              <button
                type="button"
                className="aero-btn aero-btn-danger flex-1 py-1.5 text-sm"
                onClick={resetForm}
                disabled={saving}
              >
                Cancelar
              </button>
              <button
                type="button"
                className="aero-btn aero-btn-confirm flex-1 py-1.5 text-sm font-semibold"
                onClick={() => void handleRegister()}
                disabled={saving}
              >
                {saving ? "Registrando..." : "Registrar"}
              </button>
            </div>
          </div>
        </Panel>
      </div>
    </AeroShell>
  );
}
