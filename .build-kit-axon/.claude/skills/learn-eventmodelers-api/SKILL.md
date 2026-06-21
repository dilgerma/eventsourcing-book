---
name: learn-eventmodelers-api
description: Teaches an agent everything about the eventmodelers platform API — all endpoints, their purpose, request payloads, response shapes, authentication, and element types.
---

# Eventmodelers Platform API Reference

You now have complete knowledge of the eventmodelers platform API. Use this reference whenever you need to call, implement, or reason about any endpoint.

---

## Architecture Overview

- **Framework**: Express.js + `@event-driven-io/emmett` (event sourcing)
- **Adapter**: `@event-driven-io/emmett-expressjs`
- **Database**: PostgreSQL via Knex
- **Storage / Auth**: Supabase
- **Route discovery**: Dynamic glob (`**/routes{,-*}.js`) loaded from `dist/src/slices`
- **Base URL** (local): `http://localhost:3000`

---

## Authentication & Headers

| Header | Required | Purpose |
|---|---|---|
| `Authorization` | Some routes | Supabase JWT bearer token |
| `x-user-id` | Node operations | User identifier |
| `x-causation-id` | Optional | Event causation tracing |
| `x-correlation-id` | Optional | Correlation tracing |

- CORS allowed origins: `localhost:3000`, `localhost:3001`, `https://app.eventmodelers.de`

---

## Element Types

```typescript
MODEL_CONTEXT  // Context/domain modeling container
CHAPTER        // Timeline/sequence container
ACTOR          // System participant (swimlane label)
AUTOMATION     // Automated action
API            // External service
SCREEN         // UI screen
COMMAND        // State-changing operation
EVENT          // Domain event
SPEC_ERROR     // Error scenario
TABLE          // Data table
READMODEL      // Query result / materialized view
SCENARIO       // GWT scenario
LANE           // Timeline row
SLICE_BORDER   // Slice boundary marker
```

---

## Standard HTTP Status Codes

| Code | Meaning |
|---|---|
| 200 | OK with data |
| 201 | Created |
| 204 | No content |
| 400 | Validation error / bad input |
| 401 | Authentication required |
| 404 | Resource not found |
| 409 | Conflict (e.g. duplicate) |
| 500 | Server error |

---

## 1. Boards

**File**: `src/slices/change/api-boards/routes.ts`

### POST `/api/org/:orgId/boards/:boardId/events`
Persist board/timeline row events as an array of mixed event types.

**Request body**: Array of node, comment, edge, or board events  
**Response**: `200` — processed results array

---

### GET `/api/boards`
List all boards.

**Response**: `200` — `Board[]`

---

### DELETE `/api/org/:orgId/boards/:boardId`
Delete a board.

**Response**: `204`

---

### GET `/api/org/:orgId/boards/:boardId/events/search`
Search events by node name.

**Query params**: `name` (string)  
**Response**: `200` — matching event array

---

### GET `/api/org/:orgId/boards/:boardId/events`
Get all board events in sequence.

**Response**: `200` — event array

---

### GET `/api/org/:orgId/boards/:boardId/nodes/:nodeId/comments`
Get all comments for a node.

**Response**: `200` — comment array

---

### POST `/api/org/:orgId/boards/:boardId/bucket`
Create a Supabase storage bucket for the board.

**Response**: `200` — `{ ok: boolean, bucket: string, alreadyExisted: boolean }`

---

## 2. Chapters & Timelines

**File**: `src/slices/change/api-chapters/routes.ts`

### POST `/api/org/:orgId/boards/:boardId/chapters`
Create a chapter node.

**Request body**: `{ position?: { x: number, y: number } }`  
**Response**: `200` — chapter data

---

### POST `/api/org/:orgId/boards/:boardId/timelines/:timelineId/columns`
Add a column to a timeline.

**Request body**: `{ index?: number }` (integer index, optional)  
**Response**: `200` — `{ columnId: string, index: number, totalColumns: number }`

---

### DELETE `/api/org/:orgId/boards/:boardId/timelines/:timelineId/columns/:columnId`
Delete a column from a timeline. Removes the column and all its cells. Cannot delete the last column.

**Response**:
- `200` — `{ columnId: string, totalColumns: number }`
- `400` — validation error (e.g. last column)
- `404` — timeline or column not found

---

### POST `/api/org/:orgId/boards/:boardId/timelines/:timelineId/lanes`
Add a lane (row) to a timeline.

**Request body**:
```typescript
{
  type: 'actor' | 'interaction' | 'swimlane' | 'spec' | 'feedback'
  label?: string
  index?: number
  height?: number
}
```
**Response**: `200` — lane data

---

### POST `/api/org/:orgId/boards/:boardId/timelines/:timelineId/cells/:cellId/drop`
Drop a node into a timeline cell. Validates placement rules.

**Request body**: `{ nodeId: string, nodeType: ElementType }`

**Placement rules**:
- `swimlane` lane → accepts `EVENT`
- `interaction` lane → accepts `COMMAND`, `READMODEL`
- `actor` lane → accepts `SCREEN`, `AUTOMATION`
- `feedback` lane → accepts markdown
- `spec` lane → accepts `SPEC_NODE`

**Response**:
- `200` — drop result
- `400` — placement violation
- `404` — cell or node not found

---

## 3. Nodes

**File**: `src/slices/change/api-nodes/routes.ts`

All node endpoints require header: `x-user-id`

### POST `/api/org/:orgId/boards/:boardId/nodes/events`
Submit node change events.

**Request body**: `NodeChangeEvent[]`

```typescript
interface NodeChangeEvent {
  id: string                    // uuid
  eventType: 'node:created' | 'node:changed' | 'node:deleted'
  nodeId: string
  boardId: string
  timestamp: number             // unix ms
  userId?: string
  hash?: string                 // content hash
  changedAttributes?: string[]  // dot-paths e.g. 'meta.title'
  node?: {
    id: string
    data: {
      backgroundColor?: string
      title?: string
      type?: string
      url?: string
      // ...other node data fields
    }
  }
  meta?: {
    type: ElementType
    title?: string
    description?: string
    fields?: Record<string, unknown>
    // ...
  }
  edges?: Array<{
    id: string
    source: string
    target: string
    sourceHandle?: string
    targetHandle?: string
  }>
  chapterId?: string   // for cell placement
  cellName?: string    // spreadsheet-style e.g. "B2"
}
```

**Response**: `200` — `{ hashes: { [eventId: string]: string } }`

---

### GET `/api/org/:orgId/boards/:boardId/nodes`
List all nodes on a board.

**Query params**: `type?: ElementType`  
**Response**: `200` — node record array

---

### GET `/api/org/:orgId/boards/:boardId/nodes/:nodeId`
Get a single node.

**Response**: `200` — node record OR `404`

---

## 4. Images

**File**: `src/slices/change/api-images/routes.ts`

### POST `/api/org/:orgId/boards/:boardId/images/:imageId`
Update a board image.

**Request**: `multipart/form-data` — field `file` (binary)  
**Response**: `204`

---

### POST `/api/org/:orgId/boards/:boardId/imagesnapshots/:imageId`
Update an image snapshot.

**Request**: `multipart/form-data` — field `file` (binary)  
**Response**: `204`

---

### POST `/api/org/:orgId/boards/:boardId/image-nodes/:nodeId`
Create an image node.

**Request**: `multipart/form-data` — fields: `file`, `chapterId`, `cellName`  
**Response**: `204`

---

### POST `/api/org/:orgId/boards/:boardId/images/:imageId/sketch`
Render a sketch description to WebP and upload.

**Request body**:
```typescript
{
  elements: object[]           // sketch element descriptors
  semanticDescription?: string // human-readable description stored in metadata
}
```
**Response**: `204`

---

### POST `/api/org/:orgId/boards/:boardId/image-nodes/:nodeId/sketch`
Create a SCREEN node from a sketch description.

**Request body**:
```typescript
{
  chapterId: string
  cellName: string
  description: { elements: object[] }
  semanticDescription?: string
}
```
**Response**: `204` OR `400` (validation error)

---

## 5. Slices

**File**: `src/slices/change/api-.slices/routes.ts`

### POST `/api/org/:orgId/boards/:boardId/timelines/:timelineId/slices`
Create a complete slice (1 column + 3 nodes automatically placed).

**Request body**:
```typescript
{
  type: 'state-change' | 'state-view' | 'automation'
  index?: number
  nodes?: {
    actor?: Partial<NodeData>
    interaction?: Partial<NodeData>
    swimlane?: Partial<NodeData>
  }
}
```

**Slice node mapping**:
- `state-change` → SCREEN (actor) + COMMAND (interaction) + EVENT (swimlane)
- `state-view` → SCREEN (actor) + READMODEL (interaction) + EVENT (swimlane)
- `automation` → AUTOMATION (actor) + COMMAND (interaction) + EVENT (swimlane)

**Response**: `200` — slice data

---

## 6. Specifications (GWT Scenarios)

**File**: `src/slices/change/api-specs/routes.ts`

### POST `/api/org/:orgId/boards/:boardId/contexts/:contextName/slices/:sliceName/scenarios`
Append a Given-When-Then scenario to a spec node.

**Request body**:
```typescript
{
  id: string
  title: string
  vertical?: boolean
  examples?: unknown[]
  given: string[]   // nodeIds — must be EVENTs from same timeline
  when: string[]    // nodeIds — at most one COMMAND; empty if then has READMODEL
  then: string[]    // nodeIds — EVENTs only OR exactly one READMODEL (not mixed)
}
```

**Validation rules**:
- `given`: only EVENTs from same timeline
- `when`: max one COMMAND; must be empty when `then` contains a READMODEL
- `then`: all EVENTs OR exactly one READMODEL — never mixed
- All referenced nodes must belong to the same chapter/timeline

**Response**:
- `201` — `{ scenario, scenarios, specNodeId, isNewNode: boolean }`
- `400` — validation error
- `404` — context or slice not found
- `409` — duplicate scenario title

---

### GET `/api/org/:orgId/boards/:boardId/contexts/:contextName/spec-info`
Get valid elements for a context (by name lookup).

**Response**: `200` — `{ chapterId: string, elements: ElementRecord[] }`

---

### GET `/api/org/:orgId/boards/:boardId/contexts/:contextName/slices/:sliceName/spec-info`
Get valid elements for a specific slice.

**Response**: `200` — `{ chapterId: string, elements: ElementRecord[] }`

---

## 7. Config Import

**File**: `src/slices/change/config-import/routes.ts`

### POST `/api/org/:orgId/boards/:boardId/import-config`
Import an EventModelingJson config to populate a board.

**Request**: `multipart/form-data` with field `file` OR `application/json` body:
```typescript
{ slices: SliceDefinition[] }
```

**Response**: `200` — transformed canvas with nodes and edges

---

## 8. Slice Data

**File**: `src/slices/slicedata/routes.ts`

### GET ` `
Build structured slice data from board state.

**Query params** (one required): `contextId` OR `contextName`; optional: `sliceId`  
**Response**: `200` — slice data matching event modeling schema

---

### GET `/api/org/:orgId/boards/:boardId/slicedata/slices`
List all slices on a board.

**Response**: `200` — `{ slices: Array<{ id: string, title: string, status: string }> }`

---

## 9. Extensions

**File**: `src/slices/extensions/routes.ts`

### GET `/api/org/:orgId/boards/:boardId/extensions`
List extension configs for a board.

**Response**: `200` — extension record array

---

### PUT `/api/org/:orgId/boards/:boardId/extensions/:type`
Enable or disable an extension.

**Request body**: `{ enabled: boolean, config?: object }`  
**Response**: `200` — updated extension config

---

## 10. Snapshots

**File**: `src/slices/Snapshots/routes.ts`

All snapshot endpoints require Supabase JWT authentication.

**Constraints**: max 3 snapshots per user, max 30-day retention, max 50 MB file size.

### GET `/api/snapshots`
List current user's snapshots.

**Response**: `200` — `Array<{ id, name, payload_id, expiry, shared }>`

---

### POST `/api/snapshots`
Create a snapshot.

**Request**: `multipart/form-data` — fields: `payloadFile` (binary), `name` (string), `retention?` (days, max 30)  
**Response**: `201` — `{ ok: true, id: string }`

---

### GET `/api/snapshots/:id`
Load a snapshot's payload.

**Response**: `200` — snapshot payload JSON

---

### PATCH `/api/snapshots/:id/share`
Share a snapshot (makes it publicly accessible).

**Response**: `200` — `{ ok: true }`

---

### DELETE `/api/snapshots/:id`
Delete a snapshot.

**Response**: `200` — `{ ok: true }`

---

## 11. User Management — Commands (Event Sourced)

All commands respond with:
```typescript
{
  ok: true
  next_expected_stream_version: number
  last_event_global_position: number
}
```

Optional headers on all: `correlation_id`, `causation_id`

### POST `/api/creategroup`
**Body**: `{ groupId: string, name: string }`  
**Event emitted**: `GroupCreated`

---

### POST `/api/inviteuser`
**Body**: `{ groupId: string, email: string, invitationId: string }`  
**Event emitted**: `UserInvited`

---

### POST `/api/acceptinvite`
**Body**: `{ userId: string, groupId: string, invitationId: string }`  
**Event emitted**: `InvitationAccepted`

---

### POST `/api/assignrole`
**Body**: `{ userId: string, groupId: string, role: string }`  
**Event emitted**: `RoleAssigned`

---

## 12. User Management — Read Models (Projections)

All require authentication. Optional query param `_id` to filter by ID.

### GET `/api/query/group-details-lookup`
Group details. Filter: `?_id=groupId`

### GET `/api/query/open-invites`
Pending invitations. Filter: `?_id=invitationId`

### GET `/api/query/user-group-assignments`
User-to-group mappings. Filter: `?_id=groupId`

### GET `/api/query/users-to-assign-to-groups`
Users available for group assignment. Filter: `?_id=userId`

---

## 13. Utility

### GET `/api/user`
Get current authenticated user info.

**Response**: `{ user_id: string, email: string, metadata: object }`

### GET `/api-docs`
Swagger UI (interactive API explorer)

### GET `/swagger.json`
OpenAPI specification (JSON)

---

## Domain Events

### Snapshot Events (`src/events/SnapshotsEvents.ts`)

```typescript
SnapshotStored          // { name, id, payloadId, expiry }
SnapshotDeleted         // { id }
SnapshotCleanedUp       // { id }
PublishedSnapshotDeleted // { id }
SnapshotShared          // { id }
SnapshotPublished       // { id, payloadId, bucket, path }
```

### User Management Events (`src/events/UserManagementEvents.ts`)

```typescript
GroupCreated          // { groupId, owner, name }
UserAssignedToGroup   // { groupId, userId }
UserInvited           // { groupId, invitationId, email }
InvitationAccepted    // { invitationId, groupId, userId }
RoleAssigned          // { groupId, userId, role }
```

All events support optional metadata: `user_id`, `correlation_id`, `causation_id`

---

## Key Source Files

| File | Purpose |
|---|---|
| `src/slices/change/types.ts` | `ElementType`, `NodeChangeEvent`, `EdgeEvent` |
| `src/slices/change/api-boards/routes.ts` | Board CRUD + event persistence |
| `src/slices/change/api-chapters/routes.ts` | Chapters, columns, lanes, cell drops |
| `src/slices/change/api-nodes/routes.ts` | Node event sourcing |
| `src/slices/change/api-images/routes.ts` | Image upload + sketch rendering |
| `src/slices/change/api-.slices/routes.ts` | Slice creation |
| `src/slices/change/api-specs/routes.ts` | GWT scenario management |
| `src/slices/change/config-import/routes.ts` | Config import |
| `src/slices/slicedata/routes.ts` | Slice data read models |
| `src/slices/extensions/routes.ts` | Extension management |
| `src/slices/Snapshots/routes.ts` | Snapshot CRUD |
| `src/slices/usermanagement/*/routes*.ts` | User management commands + projections |
| `src/events/SnapshotsEvents.ts` | Snapshot domain events |
| `src/events/UserManagementEvents.ts` | User management domain events |
| `backend/src/server.ts` | Route wiring, CORS, `/api/user` |
