import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  // Keep assets working both at a custom-domain root and on GitHub Pages' project path.
  base: "./",
  plugins: [react()],
  server: {
    allowedHosts: ["ramingo.online", "www.ramingo.online", "travelplanner.muntim.ru"],
  },
});
