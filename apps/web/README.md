# FlowSuite Web

React (Vite) + TypeScript single-page app for FlowSuite.

- **State/data:** TanStack Query against the REST API
- **Forms:** React Hook Form
- **Styling:** Tailwind CSS
- **Charts:** Recharts (usage dashboard)
- **Auth:** access + refresh JWTs in `localStorage`, with automatic refresh on 401

## Pages

Login / Register / Accept-invite · Dashboard (usage vs. limits) · Projects · Tasks ·
Customers · Members (invite / roles / remove) · Billing (plans, upgrade) · Audit log.
Navigation and actions are gated by the signed-in user's role.

## Develop

```bash
npm install
npm run dev     # http://localhost:5173, proxies /api -> http://localhost:8080
```

`npm run build` type-checks and builds; `npm run lint` runs ESLint.

Set `VITE_API_URL` to point at a non-proxied backend origin (defaults to same-origin `/api`).

## Docker

Built and served by nginx as part of the root `docker compose up` (see root README); nginx
serves the SPA and proxies `/api` to the `api` service.
