# etzhayyim-project-sos — System of System Resource Intelligence

## Identity

| Key | Value |
|---|---|
| **Domain** | `sos.etzhayyim.com` *(planned)* |
| **Nanoid** | `s0s5ys0s` |
| **App path** | `appview/etzhayyim-wasm-systemofsystem-s0s5ys0s/svelte/` |
| **Runtime** | SpinKube + SvelteKit (per PROJECT.jsonld) |
| **3D viewer** | **Threlte** (`@threlte/core` + `@threlte/extras`) — Svelte wrapper around three.js |

The canonical identity + scope is in `PROJECT.jsonld`. This file is a navigation breadcrumb for operators landing on the directory cold.

## Architecture (Threlte viewer)

This app uses **Threlte** (not the religious-corp `@etzhayyim/kami-engine-sdk`) for its 3D visualization layer. Same pattern as `60-apps/etzhayyim-project-cad/` (per its CLAUDE.md "3D viewer 標準は Threlte"). The three.js + `@threlte/*` deps in `appview/.../svelte/package.json` are documented design intent, NOT dead deps.

The SDK three-free cutover (ADR-2605264300) does NOT apply to this app:
- this app does NOT depend on `@etzhayyim/kami-engine-sdk` (no SDK in `package.json`)
- the religious-corp SDK retired its internal three.js renderer; Threlte is a separate Svelte 3D library that consumer apps may use directly

## Scope (from PROJECT.jsonld)

Threlte-driven systems-thinking app that maps:
- resource stocks
- causal loops
- leverage points
- cross-domain dependencies as a system-of-systems

## Current state

`appview/.../svelte/src/App.svelte` is a "Vite entry scaffold after SvelteKit cleanup" placeholder. The Threlte viewer + actual SoS modeling logic are out-of-scope for the current scaffold; the deps are aspirationally configured for the documented R1+ implementation work.

## References

- `PROJECT.jsonld` — canonical identity (schema.org/Project)
- `kotodama.jsonld` — kotodama actor manifest
- ADR-2605264300 (kami-engine-sdk three.js-free cutover) §2 — confirms this app's Threlte deps are KEEP not dead
- `60-apps/etzhayyim-project-cad/CLAUDE.md` — sibling Threlte-viewer pattern
