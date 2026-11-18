import { defineConfig } from '@playwright/test';

// Expects the stack to be running: backend on :8080 and `npm run dev` (frontend) on :5173.
export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: { baseURL: process.env.BASE_URL ?? 'http://127.0.0.1:5173', trace: 'retain-on-failure' },
});
