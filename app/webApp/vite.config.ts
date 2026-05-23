import { defineConfig } from "vite";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
    plugins: [
        tailwindcss(),
    ],
    server: {
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
            },
        },
    },
    build: {
        rollupOptions: {
            output: {
                assetFileNames: (assetInfo) => {
                    const name = assetInfo.names[0] ?? "";
                    if (name.endsWith(".wasm")) {
                        return "wasmJs/[name][extname]";
                    }
                    return "[name][extname]";
                },
            },
        },
    },
});
