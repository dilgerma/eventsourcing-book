---
name: connect
description: Resolve eventmodelers connection config (token, boardId, baseUrl) from inline params or .eventmodelers/config.json — ask the user for missing values, persist them, and add the file to .gitignore. All other skills invoke this first.
---

# Connect — Resolve Eventmodelers Config

**Every other skill invokes this skill first** before making any API calls. Do not proceed past this skill until all four values (`TOKEN`, `BOARD_ID`, `ORG_ID`, `BASE_URL`) are resolved.

---

## What this skill produces

After running, the following variables are available for the rest of the session:

| Variable | Header sent to API | Description |
|----------|--------------------|-------------|
| `TOKEN` | `x-token` | API token UUID |
| `BOARD_ID` | `x-board-id` | Target board UUID |
| `ORG_ID` | — | Organization UUID (used in all board-scoped URLs) |
| `BASE_URL` | — | Base URL, e.g. `http://localhost:3000` |

Every API call in every skill must include these headers:
```
x-token: <TOKEN>
x-board-id: <BOARD_ID>
x-user-id: <skill-name>   ← set by each skill individually
```

All board-scoped URLs follow the pattern: `<BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/...`

---

## Step 0 — Check for inline parameters

Before reading the config file, scan the prompt/arguments that invoked this skill for inline overrides. Supported formats:

| Pattern | Example |
|---------|---------|
| `board=<uuid>` | `board=05cda19d-d5b8-4b51-ae88-c72f2611548a` |
| `token=<uuid>` | `token=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
| `org=<uuid>` | `org=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
| `baseUrl=<url>` | `baseUrl=http://localhost:3000` |

If an inline `board=<uuid>` is found, use it as `BOARD_ID` — **it takes priority over the config file**. Same for `token`, `org`, and `baseUrl`. Record which values came from inline params so they are not overwritten in Step 3.

---

## Step 1 — Read config file

Check whether `.eventmodelers/config.json` exists in the current working directory:

```bash
cat .eventmodelers/config.json 2>/dev/null
```

If the file exists and is valid JSON, extract any values **not already set by Step 0**:
- `token` → `TOKEN`
- `boardId` → `BOARD_ID`
- `organizationId` → `ORG_ID`
- `baseUrl` → `BASE_URL` (default: `https://api.eventmodelers.de` if missing)

Resolution priority: **inline param > config file > ask user**

If all four are present (from any source), skip to **Step 4 — Verify**.

---

## Step 2 — Ask for missing values

If after Steps 0 and 1 any required field is still missing, **ask the user one question first**:

> "Do you have a config from the eventmodelers accounts page? (yes / no)"

**If the user answers yes:**
Stop asking questions. Show this hint and wait for them to paste:

> "Great — please paste your config from https://app.eventmodelers.de/account here."

When they paste a JSON object, parse it immediately — accept both `orgId` and `organizationId` as the organization field — apply all values, and proceed directly to Step 3.

**If the user answers no** (or pastes only a partial config), ask for each still-missing field one at a time, in this order: `token`, then `boardId`, then `orgId`. Wait for the answer before asking the next.

| Field | What to ask |
|-------|-------------|
| `token` | "Please provide your eventmodelers API token (a UUID from your workspace settings)." |
| `boardId` | "Please provide the board ID you want to work with (the UUID from the board URL)." |
| `orgId` | "Please provide your organization ID (the UUID from your organization settings)." |
| `baseUrl` | Do **not** ask — default to `https://api.eventmodelers.de` silently. |

Where to find the token: users generate API tokens in their workspace settings at the eventmodelers platform. The token is shown only once at creation time. It is a UUID and must belong to the same organization as the board.

---

## Step 3 — Persist config

Once all values are collected, write the config file. When writing, merge with any existing config — do **not** overwrite fields that were provided as inline params with values from a previous config (the inline param is the user's explicit intent for this session, but the persisted value should reflect the most recently user-supplied value):

```bash
mkdir -p .eventmodelers
cat > .eventmodelers/config.json << 'EOF'
{
  "token": "<TOKEN>",
  "boardId": "<BOARD_ID>",
  "orgId": "<ORG_ID>",
  "baseUrl": "<BASE_URL>"
}
EOF
```

Then ensure `.eventmodelers/config.json` is in `.gitignore`. Check whether it is already present:

```bash
grep -q ".eventmodelers/config.json" .gitignore 2>/dev/null || echo "MISSING"
```

If `MISSING`, append it:

```bash
echo ".eventmodelers/config.json" >> .gitignore
```

Tell the user: `"Config saved to .eventmodelers/config.json and added to .gitignore."`

---

## Step 4 — Verify

Confirm the token and board are valid with a lightweight call:

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -H "x-token: <TOKEN>" \
  -H "x-board-id: <BOARD_ID>" \
  -H "x-user-id: connect-skill" \
  "<BASE_URL>/api/org/<ORG_ID>/boards/<BOARD_ID>/nodes?type=CHAPTER"
```

| Response | Action |
|----------|--------|
| `200` | Config is valid. Print one line: `"Connected — board <BOARD_ID>"` and return. |
| `401` | Token is invalid or missing. Tell the user and re-run from Step 2, clearing `token`. |
| `403` | Token organization does not match board. Tell the user to check that the token was issued for the correct workspace. Re-run from Step 2 for both fields. |
| `404` | Board not found. Tell the user and re-run from Step 2, clearing `boardId`. |
| Any other | Print the status code and raw response. Ask the user how to proceed. |

---

## Config file format

`.eventmodelers/config.json`:
```json
{
  "token": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "boardId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "orgId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "baseUrl": "http://localhost:3000"
}
```

The `token` field is a secret. It is never logged or shown after initial confirmation.

---

## Security notes

- The config file is workspace-local and gitignored — never commit it.
- The token grants write access to all boards in its organization — treat it like a password.
- If a skill receives a `401` or `403` mid-session, re-invoke this skill to refresh the config before retrying.
