# Transfer Flow and Requirements

This document describes the expected transfer behavior for backend execution in `TransferServiceImpl` and `TransferExecutorServiceImpl`.

## Scope

- Covers wallet and item transfers used by commands like gift, rain, sell, send, and fish.
- Defines special handling for `BOT_USER_ID` and system user `"0"`.
- Documents invariants that must hold even if frontend checks are bypassed.

## Core Concepts

- **Primary transfer engine**: `TransferServiceImpl.transfer(...)`
- **Transactional executor with optimistic locking**: `TransferExecutorServiceImpl.executeTransfer(...)`
- **Persistence behavior for special users**:
  - `UserWalletsServiceImpl.saveWallet(...)` does not persist entities for `"0"` or `BOT_USER_ID`.
  - `UserItemsServiceImpl.saveItem(...)` does not persist entities for `"0"` or `BOT_USER_ID`.

## Special User Semantics

- **`BOT_USER_ID`**
  - Represents guild reserve interaction.
  - Transfers **to** bot credit the guild wallet (`guildWallets`).
  - Bot wallet/item entities are not required for correctness.
  - Guild reserve must never go negative.

- **System user `"0"`**
  - Represents mint/burn boundary for system operations.
  - Transfer **to** `"0"` burns from sender (withdrawal/sink behavior).
  - Transfer **from** `"0"` mints to receiver (deposit/source behavior).
  - No wallet/item entity is required for system user `"0"`.

## Item Rules

- All transfer items are treated as creatures.
- Every creature maps to exactly one currency ticker.
- Creature values are stored in raw units.
- Sale/credit value is quantity-based and uses configured creature raw value and bonus multipliers logic.

## Guild Wallet Rules

- If a guild wallet entity does not exist during a bot-directed credit, backend must create it and then apply the credit.
- Bot-directed credits must be additive and ticker-specific.
- Bot-origin deductions must check guild wallet first and must fail if insufficient.

## Conservation and Divisibility Requirements

- For divisible transfer across receivers, backend uses integer division in raw units.
- Total deducted amount must match the distributed amount (`perReceiver * receiverCount`) to avoid fund burn from remainders.
- Required assets that cannot be split at least `1` per receiver should fail with indivisible transfer error.

## Receiver Handling

- List of receiver IDs should be unique.
- Backend defensively handles receiver sets by counting unique receivers for split calculations.
- Empty receiver set is invalid.

## Execution Flow (High Level)

1. Parse and normalize transfer input (`processInputs`).
2. Fetch working sets of user wallets/items and guild wallets in executor.
3. Apply primary transfer mutations in-memory.
4. Apply secondary transfer mutations in-memory (if present).
5. Persist allowed entities (excluding `"0"` and `BOT_USER_ID` in user collections).
6. Persist transaction record.

`executeTransfer` retries the whole unit three times on
`OptimisticLockingFailureException` (concurrent wallet writes). That already
covers the Mongo write collisions that show up in production, including
deposit mints. **Later:** tighten that retry so a failed persist cannot leave
a half-applied transfer (for example a `depositRecords` row with no ledger
credit). Do not special-case deposits; improve the shared transactional
retry.

## Backend vs Frontend Validation

- Frontend command checks (for example, fish reserve prechecks) are useful UX guards.
- Backend remains the source of truth and must enforce non-negative guild reserve and conservation invariants regardless of caller behavior.

## Non-Goals

- This document does not define command UX messages or frontend interaction flow.
- This document does not replace per-command limits (minimums, role checks, random limits, etc.).

