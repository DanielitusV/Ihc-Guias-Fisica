import { createFileRoute } from "@tanstack/react-router";
import { useCallback, useEffect, useState } from "react";
import { AeroShell, GuiaBadge, Panel } from "@/components/aero-shell";
import { supabase } from "@/lib/supabase";

export const Route = createFileRoute("/inventario")({
  component: InventarioPage,
  head: () => ({ meta: [{ title: "Inventario · CEF Guías" }] }),
});

type GuiaTipo = "Gral" | "I" | "II" | "III";

type GuideRecord = {
  id: number;
  name: string;
  subject: string;
  stock: number;
};

type InventoryMovementRecord = {
  guide_id: number;
  type: "entrada" | "salida" | "ajuste";
  quantity: number;
  note: string | null;
};

type InventoryRow = {
  id: number;
  tipo: GuiaTipo;
  compradas: number;
  efectivo: number;
  qr: number;
  stock: number;
  estado: "Reponer" | "OK";
};

const emptyMovementStats = () => ({
  compradas: 0,
  efectivo: 0,
  qr: 0,
});

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

function buildInventoryRows(
  guides: GuideRecord[],
  movements: InventoryMovementRecord[],
): InventoryRow[] {
  const movementsByGuide = new Map<number, ReturnType<typeof emptyMovementStats>>();

  movements.forEach((movement) => {
    const stats = movementsByGuide.get(movement.guide_id) ?? emptyMovementStats();
    const quantity = Number(movement.quantity) || 0;
    const note = normalizeText(movement.note ?? "");

    if (movement.type === "entrada") {
      stats.compradas += quantity;
    }

    if (movement.type === "salida") {
      if (note.includes("QR")) {
        stats.qr += quantity;
      } else if (note.includes("EFECTIVO")) {
        stats.efectivo += quantity;
      }
    }

    movementsByGuide.set(movement.guide_id, stats);
  });

  return guides.map((guide) => {
    const stock = Number(guide.stock) || 0;
    const stats = movementsByGuide.get(guide.id) ?? emptyMovementStats();

    return {
      id: guide.id,
      tipo: getGuideTipo(guide),
      compradas: stats.compradas,
      efectivo: stats.efectivo,
      qr: stats.qr,
      stock,
      estado: stock < 10 ? "Reponer" : "OK",
    };
  });
}

function InventarioPage() {
  const [rows, setRows] = useState<InventoryRow[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadInventory = useCallback(async () => {
    setLoading(true);
    setError(null);

    const [guidesResponse, movementsResponse] = await Promise.all([
      supabase.from("guides").select("id, name, subject, stock").order("id", { ascending: true }),
      supabase
        .from("inventory_movements")
        .select("guide_id, type, quantity, note")
        .order("created_at", { ascending: false }),
    ]);

    if (guidesResponse.error) {
      setRows([]);
      setError(guidesResponse.error.message);
      setLoading(false);
      return;
    }

    if (movementsResponse.error) {
      setRows([]);
      setError(movementsResponse.error.message);
      setLoading(false);
      return;
    }

    setRows(
      buildInventoryRows(
        (guidesResponse.data ?? []) as GuideRecord[],
        (movementsResponse.data ?? []) as InventoryMovementRecord[],
      ),
    );
    setLoading(false);
  }, []);

  useEffect(() => {
    void loadInventory();
  }, [loadInventory]);

  return (
    <AeroShell title="Inventario" subtitle="Stock real por guia y ventas registradas">
      <Panel title="Detalle de inventario por guia">
        <table className="aero-table">
          <thead>
            <tr>
              <th>Guia</th>
              <th>Compradas</th>
              <th>Vendidas efectivo</th>
              <th>Vendidas QR</th>
              <th>Stock restante</th>
              <th>Estado</th>
            </tr>
          </thead>
          <tbody>
            {loading && (
              <tr>
                <td colSpan={6} className="text-center text-xs">
                  Cargando inventario...
                </td>
              </tr>
            )}
            {!loading && error && (
              <tr>
                <td colSpan={6} className="text-center text-xs text-[oklch(0.5_0.2_25)]">
                  No se pudo cargar el inventario: {error}
                </td>
              </tr>
            )}
            {!loading && !error && rows.length === 0 && (
              <tr>
                <td colSpan={6} className="text-center text-xs">
                  Sin guias registradas.
                </td>
              </tr>
            )}
            {rows.map((r) => (
              <tr key={r.id}>
                <td>
                  <GuiaBadge tipo={r.tipo} />
                </td>
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
          Stock restante segun entradas y ventas registradas. En rojo: stock bajo.
        </p>
      </Panel>
    </AeroShell>
  );
}
