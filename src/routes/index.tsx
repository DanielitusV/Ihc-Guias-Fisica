import { createFileRoute } from "@tanstack/react-router";
import { ConsultaPage } from "./consulta";

export const Route = createFileRoute("/")({
  component: HomePage,
  head: () => ({ meta: [{ title: "Consulta de guias - CEF UMSS" }] }),
});

function HomePage() {
  return <ConsultaPage showLogin />;
}
