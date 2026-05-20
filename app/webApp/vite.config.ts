import { defineConfig } from "vite";

export default defineConfig({
    server: {
        proxy: {
            "/api": {
                target: "http://localhost:8080",
                changeOrigin: true,
            },
        },
    },
    plugins: [
        {
            name: "wasm-mime",
            configureServer(server) {
                server.middlewares.use((_req, res, next) => {
                    if (_req.url?.endsWith(".wasm")) {
                        res.setHeader("Content-Type", "application/wasm");
                    }
                    next();
                });
            },
        },
    ],
    build: {
        rollupOptions: {
            output: {
                assetFileNames: (assetInfo) => {
                    if (assetInfo.name?.endsWith(".wasm")) {
                        return "wasmJs/[name][extname]";
                    }
                    return "[name][extname]";
                },
            },
        },
    },
});
