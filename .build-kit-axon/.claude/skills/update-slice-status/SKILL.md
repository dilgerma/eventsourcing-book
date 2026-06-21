---
name: update-slice-status
description: Update the status of a single slice on an eventmodelers board by changing the SLICE_BORDER node's sliceStatus field
---

# Update Slice Status

> **Before doing anything else**, invoke the `connect` skill to resolve `TOKEN`, `BOARD_ID`, `ORG_ID`, and `BASE_URL`. Do not proceed until the connect skill has completed.

---

## Step 1 — Parse arguments

From `$ARGUMENTS`, extract:

| Field | How to find it | Default |
|-------|---------------|---------|
| `sliceName` | the slice title to update (case-insensitive match) | **required** |
| `newStatus` | the target status value | **required** |

Valid status values (case-sensitive):

| Value | Meaning |
|-------|---------|
| `Created` | Default — slice has been created but not started |
| `Planned` | Work is planned |
| `InProgress` | Work is actively in progress |
| `Review` | Ready for review |
| `Done` | Completed |
| `Blocked` | Blocked by something |
| `Assigned` | Assigned to someone |
| `Informational` | Informational / reference slice |

If `newStatus` is not one of these exact values, stop and tell the user the valid options.

---

## Step 2 — List all slices

Fetch all slices on the board:

```bash
curl -s \
  -H "x-token: <TOKEN>" \
  -H "x-board-id: <BOARD_ID>" \
  -H "x-user-id: update-slice-status-skill" \
  "<BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/slicedata/slices"
```

Response: `{ "slices": [{ "id": "<nodeId>", "title": "<title>", "status": "<status>" }] }`

The `id` here is the `SLICE_BORDER` node ID — use it directly in Step 3.

Find the slice whose `title` matches `sliceName` (case-insensitive). If no match is found, stop and list the available slice titles so the user can pick one.

Save the matched slice as:
- `SLICE_NODE_ID` — the node ID of the SLICE_BORDER
- `CURRENT_STATUS` — the current status value

---

## Step 3 — Update the slice status

Send a `node:changed` event to update the `sliceStatus` field in the SLICE_BORDER node's meta:

```bash
curl -s -X POST "<BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes/events" \
  -H "Content-Type: application/json" \
  -H "x-token: <TOKEN>" \
  -H "x-board-id: <BOARD_ID>" \
  -H "x-user-id: update-slice-status-skill" \
  -d '[{
    "id": "<new-random-uuid>",
    "eventType": "node:changed",
    "nodeId": "<SLICE_NODE_ID>",
    "boardId": "<BOARD_ID>",
    "timestamp": <Date.now()>,
    "changedAttributes": ["sliceStatus"],
    "meta": {
      "sliceStatus": "<newStatus>"
    }
  }]'
```

Response: `{ "hashes": { "<eventId>": "<hash>" } }`

---

## Step 4 — Report back

Tell the user:

- **Slice**: the title that was updated
- **Previous status**: `CURRENT_STATUS`
- **New status**: `newStatus`
- **Node ID**: `SLICE_NODE_ID`
- **Any errors**: raw API message if something failed

Example success output:
```
Updated: "Order Placed" slice
  Before: InProgress
  After:  Done
Node ID: a1b2c3d4-…
```