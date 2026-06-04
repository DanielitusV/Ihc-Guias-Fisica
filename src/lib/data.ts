import { supabase } from "./supabase";
 
// ---- Tipos del dominio --------------------------------------------------
 
export type GuiaTipo = "Gral" | "I" | "II" | "III";
 
// Fila real de la tabla `guides` (esquema existente, sin cambios)
export interface GuideRow {
  id: number;
  name: string;
  subject: string;
  price: number;
  stock: number;
  created_at: string;
}
 
// Guía enriquecida para la UI: le derivamos el `tipo` a partir del subject/name.
export interface Guide extends GuideRow {
  tipo: GuiaTipo;
  careers: string;
}
 
// ---- Mapeos derivados (porque la tabla no tiene columna `type`) ----------
 
/** Deduce el tipo de guía (Gral/I/II/III) a partir del texto de la materia. */
export function tipoFromText(text: string): GuiaTipo {
  const t = text.toLowerCase();
  // El orden importa: III antes que II, y II antes que I.
  if (t.includes("iii") || t.includes(" 3") || t.includes("basica iii")) return "III";
  if (t.includes("ii") || t.includes(" 2") || t.includes("basica ii")) return "II";
  if (t.includes(" i") || t.includes(" 1") || t.includes("basica i")) return "I";
  return "Gral";
}
 
const CAREERS: Record<GuiaTipo, string> = {
  Gral: "Civil, Industrial, Química, Alimentos, Mecánica, Agroindustrial",
  I: "Informática, Sistemas, Electrónica, Eléctrica, Lic. Física, Lic. Matemática",
  II: "Informática, Sistemas, Electrónica, Eléctrica, Lic. Física, Lic. Matemática",
  III: "Electrónica, Eléctrica, Lic. Física",
};
 
function enrich(row: GuideRow): Guide {
  const tipo = tipoFromText(`${row.subject} ${row.name}`);
  return { ...row, tipo, careers: CAREERS[tipo] };
}
 
// ---- Lectura ------------------------------------------------------------
 
/** Lista las guías, ordenadas por id, con tipo y carreras derivados. */
export async function fetchGuides(): Promise<Guide[]> {
  const { data, error } = await supabase
    .from("guides")
    .select("*")
    .order("id", { ascending: true });
 
  if (error) throw new Error(error.message);
  return (data ?? []).map(enrich);
}
 
// ---- Escritura (entrega) ------------------------------------------------
 
export interface RegistrarEntregaInput {
  guideId: number;
  quantity: number;
  method: "efectivo" | "qr";
  student?: string;
}
 
/**
 * Registra una entrega usando SOLO las tablas existentes:
 *   1) lee la guía y valida stock
 *   2) descuenta el stock en `guides`
 *   3) deja constancia en `inventory_movements` (type = 'salida')
 *
 * Nota: sin una función RPC del lado del servidor no es 100% atómico,
 * pero para el prototipo es suficiente. Si el insert del movimiento
 * fallara tras descontar el stock, se revierte el descuento.
 */
export async function registrarEntrega(input: RegistrarEntregaInput) {
  const { guideId, quantity, method, student } = input;
 
  if (quantity <= 0) throw new Error("La cantidad debe ser mayor a 0");
 
  // 1) leer guía actual
  const { data: guide, error: readErr } = await supabase
    .from("guides")
    .select("*")
    .eq("id", guideId)
    .single();
 
  if (readErr) throw new Error(readErr.message);
  if (!guide) throw new Error("La guía no existe");
  if (guide.stock < quantity) {
    throw new Error(`Stock insuficiente: hay ${guide.stock} y se piden ${quantity}`);
  }
 
  // 2) descontar stock
  const nuevoStock = guide.stock - quantity;
  const { error: updErr } = await supabase
    .from("guides")
    .update({ stock: nuevoStock })
    .eq("id", guideId);
 
  if (updErr) throw new Error(updErr.message);
 
  // 3) registrar movimiento de salida
  const nota = `Entrega · ${method}${student ? ` · ${student}` : ""}`;
  const { error: movErr } = await supabase.from("inventory_movements").insert({
    guide_id: guideId,
    type: "salida",
    quantity,
    note: nota,
  });
 
  // si falla el movimiento, revertimos el stock para no descuadrar
  if (movErr) {
    await supabase.from("guides").update({ stock: guide.stock }).eq("id", guideId);
    throw new Error(movErr.message);
  }
 
  return {
    guide_id: guideId,
    quantity,
    unit_price: guide.price,
    total: guide.price * quantity,
    method,
    student: student ?? null,
  };
}
 