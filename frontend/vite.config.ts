/// <reference types="vitest" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev proxy keeps the browser same-origin with the API (no CORS needed in dev).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.API_URL ?? 'http://localhost:8080',
        configure: (proxy) => {
          // In dev the browser talks to Vite only, so the call is effectively same-origin. Forwarding the
          // browser's Origin header would make the backend's (deliberately strict) CORS check reject POST/PATCH
          // whenever the app is opened via 127.0.0.1 or a LAN address instead of localhost.
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'));
        },
      },
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
  },
});
