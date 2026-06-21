# Agent Learnings

Patterns and gotchas discovered during task processing. Update this file whenever you encounter something reusable.

## tasks.json

- Tasks are objects with `id`, `createdAt`, and `payload` (a `SliceChangedPayload`).
- After completing a task, remove it from the array entirely — do not add a status field.
- Write `[]` to `tasks.json` if the last task is completed.

## SliceChangedPayload fields

```
event           always "slice:changed"
organizationId  org UUID or null
boardId         board UUID
sliceId         SLICE_BORDER node UUID — use this with /load-slice
sliceTitle      human-readable slice name (may be null)
sliceStatus     e.g. "Created", "InProgress", "Done", "Blocked" (may be null)
timestamp       unix ms when the change was emitted
```

## Slice files

The realtime agent writes one file per slice on startup and after each `slice:changed` event:

```
.slices/<context>/<sliceName>/slice.json
```

- `<context>` is the slice's context value, or `default` if none.
- `<sliceName>` is the slice title lowercased with spaces removed (e.g. `"Enable User"` → `enableuser`).

These files are always up to date — read them directly before invoking any skill.

## Skill Usage

- Always run `/connect` first to load credentials from `.eventmodelers/config.json` before calling any other skill.
- `/load-slice sliceId=<uuid>` re-fetches all slices from the API, refreshes the slice files, and returns the requested slice. Use it when you need a guaranteed-fresh view of a specific slice.
- Read `.slices/<context>/<sliceName>/slice.json` directly when you already know the context and name and the file is recent enough.

## Board API

- The `boardId` and `organizationId` from each payload provide full context — pass them to skills.
- Node events use `node:created`, `node:changed`, `node:deleted` — always POST to `/api/org/:orgId/boards/:boardId/nodes/events`.
- Slice metadata (title, status) lives on the SLICE_BORDER node under `meta.sliceStatus` and `meta.title`.
