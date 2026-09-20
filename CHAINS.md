# Chain Integration: Nodes, Deposits, Withdrawals and Representatives

This document describes how the backend talks to each network. `TRANSFERS.md`
covers the internal ledger; everything here is what happens on the far side of
it, where value actually moves on a chain.

## Scope

- Covers deposit detection, withdrawal submission, confirmation and refunds.
- Covers what differs per protocol and, more importantly, what does not.
- Documents the invariants that keep a user from being paid twice or losing a
  debited balance.

## Core Concepts

- **`ChainAdapter`** - one implementation per network family. Owns everything
  protocol-specific: how a deposit address is obtained, how deposits are
  detected, how withdrawals are signed and broadcast.
- **`ChainAdapterRegistry`** - resolves the adapter for a currency from
  `CurrencyEntity.protocol`. A document with no protocol resolves to `NANO`,
  which is what keeps pre-Monero currency documents working.
- **`ChainLedgerService`** - the protocol-agnostic half. Crediting a deposit,
  notifying a confirmed withdrawal and refunding a failed one are identical on
  every network, so adapters delegate rather than reimplement.
- **Everything above the adapter is plain ledger arithmetic.** Balances, gifts,
  rain, drops and fishing never know which chain they are denominated in.

## Protocols at a Glance

| | Nano / Banano | Monero | Bitcoin |
|---|---|---|---|
| Protocol value | `NANO` | `MONERO` | `BITCOIN` |
| Network fee | none | yes | yes |
| Deposit address | derived from the user's seed | subaddress of the hot wallet | `getnewaddress` on the hot wallet |
| Deposit detection | websocket push plus sweep | `get_transfers` over a height window | `listsinceblock` over a height window |
| Needs a sweep to the hot wallet | yes | no | no |
| Default confirmations | 1 | 10 | 6 |
| Representative | yes | no | no |

Nano is the outlier in both directions: it is the only feeless one, and the only
one where a deposit lands somewhere other than the hot wallet and so has to be
consolidated afterwards.

## Driving Loop

- `CronJobs.checkActivity` runs on a one second fixed delay.
- `NodesServiceImpl.checkActivity` iterates every currency, resolves its adapter
  and calls `processActivity`.
- The scheduler pool is single threaded and the delay is measured from
  completion, so a pass never overlaps itself.
- Each currency is wrapped in its own try/catch. One chain's failure must not
  abandon the rest of the pass.
- Adapters must be cheap when idle. Each gates its expensive work on the chain
  having actually advanced.

## Deposit Addresses

- Nano and Banano derive the address from the user's seed, so it can always be
  recomputed and nothing needs storing.
- Monero and Bitcoin allocate from a single hot wallet and store the mapping in
  `depositAddresses`, because the wallet identifies incoming funds by index or
  address rather than by owner.
- `depositAddresses` is unique on `(ticker, userId)`, `(ticker, address)` and
  `(ticker, addressIndex)`. A user gets one address per currency, forever.
- Bitcoin takes its index from the `hdkeypath` the node reports, so the node
  guarantees uniqueness and no counter of our own is needed. Index 0 is burned
  at wallet setup so a user index is never zero, matching Monero's convention
  that index 0 is the wallet's own address.
- An address that cannot be allocated returns null and the currency is omitted,
  rather than showing an address that cannot receive funds.

## Deposit Detection

Every adapter follows the same shape:

1. Ask the node for its height. Skip the pass if it is unreachable or syncing.
2. Scan a window from the stored cursor back a few blocks, to survive a reorg.
3. Credit anything that has reached the required confirmations.
4. Advance the cursor only to `height - confirmations`, never to the tip, so an
   output is never skipped before it is old enough to credit.

On top of that, Monero and Bitcoin re-scan their entire history every ten
minutes. The windowed scan only looks a short way back, so a deposit maturing
while the API was down for longer than that would fall out of range permanently.
Nano gets the same protection from its periodic account sweep.

Bitcoin additionally sums multiple outputs paying the same address in one
transaction. Deposit records are keyed per address, so crediting them
individually would book the first and silently drop the rest.

### Discovery Notices

Bitcoin and Monero tell the user about a deposit before it can be credited, so a
transfer that takes an hour to settle does not look lost in the meantime:

- Bitcoin runs its `listsinceblock` window every ten seconds even when no block
  has arrived, and anything in the `receive` category with fewer than the
  required confirmations (and not fewer than zero, which means conflicted) is a
  discovery.
- Monero asks the wallet for its transaction pool every ten seconds, and also
  treats an `in` transfer that is mined but not yet ten deep as a discovery when
  the block-gated scan sees one.
- Nano has no such phase. A deposit is only ever seen once it is confirmed, so
  discovery and crediting are the same moment and there is nothing to announce
  early.

A discovery is announced exactly once. `depositNotices` is unique on
`(ticker, txid, addressIndex)` and the row is inserted before the message is
written, the same insert-or-skip pattern as crediting, so re-seeing the same
transaction on every scan costs nothing. It is a separate collection from
`depositRecords` on purpose: a row there means money moved, and the credit paths
must never have to ask which kind of row they are looking at. A deposit that
already has a record is never announced as discovered, since a "discovered"
after a "confirmed" would read as a second deposit. Notice rows expire after a
week.

## Exactly-Once Crediting

- `depositRecords` is unique on `(ticker, txid, addressIndex)`.
- The record is inserted **before** the ledger credit. A duplicate insert fails
  and the credit is skipped.
- That ordering is what makes re-scanning safe, which is what makes the
  reconciliation sweeps safe to run as often as they do.
- Nano needs this as much as the others: the node repeats confirmations for a
  hash and replays them across a reconnect.

## Withdrawals

1. `/send` validates the address and the minimum, then debits the user
   immediately and creates a `SEND` queue entry.
2. The adapter picks the entry up, builds and broadcasts the transaction, and
   records the resulting hash. The user is told the withdrawal has been *sent*,
   with the hash and how many confirmations it needs.
3. Once confirmed, the queue entry is deleted and the user is told it has been
   *confirmed*.

The balance is gone from the moment the entry is created. Everything below
exists because of that.

The "sent" notice goes out on every protocol, Nano included even though its
confirmation usually follows within a second or two. The pair of messages shows
how fast the network is, and when the node is slow to see the confirmation the
user still has proof that their `/send` went out. It is only written on a fresh
broadcast: the recovery paths cannot tell whether it was announced before the
crash, and a second "sent" would look like a second withdrawal.

A Monero withdrawal to one of the bot's own deposit addresses confirms both a
withdrawal and a deposit at once. The deposit is credited on the ledger before
the queue entry is dropped, since that order is what keeps a crash in between
recoverable, but its notice is held back until after the withdrawal notice so
the two messages read in the order things happened.

### Broadcast Safety

Before an adapter resends anything, it must establish which of three states it
is in. Collapsing the last two is how a user gets paid twice.

| Outcome | Meaning | Action |
|---|---|---|
| sent | found on the network | adopt that transaction, never resend |
| not sent | proven absent | safe to build and send |
| unknown | the node could not be asked | wait; assume nothing |

`BroadcastCheck` carries this, and each protocol proves it differently:

- **Nano** records the block hash *before* publishing, which it can do because a
  state block's hash is fully determined by its contents. Recovery is then an
  exact `block_info` lookup. If nothing was published the frontier is unchanged,
  so the rebuilt block is byte for byte the one already recorded.
- **Bitcoin** stamps `nanobot:<queueId>` into the wallet comment and matches on
  that. Matching on amount would be wrong, because the fee is taken out of the
  output.
- **Monero** matches the destination address and adds the transfer fee back
  before comparing, for the same reason.

### Failure and Refunds

- A send that the node *rejected* counts an attempt. A send that could not be
  attempted, because the node or the work server was unreachable, does not. A
  brief outage must not cancel a good withdrawal.
- After three counted attempts the entry is refunded, but only if the broadcast
  check is conclusive and found nothing. An inconclusive check holds the entry
  for review instead.
- The refund restores the full debited amount, fee portion included, and the
  queue entry is only removed once the refund is booked.
- Sweeps have no user behind them and are never refunded.

### Fees

- Fee-bearing sends subtract the fee from the output. The user is debited the
  full requested amount and receives that amount minus the fee, so the hot
  wallet drops by exactly what was burned from the balance and the ledger stays
  balanced.
- `feeEstimate` is stored separately from `minimumWithdraw`. Folding it in would
  compound on every refresh and drive the minimum up without bound. The
  effective minimum is the sum of the two.
- A fee-bearing currency with no usable estimate refuses withdrawals rather than
  falling back to the bare minimum, which would accept an amount the fee will
  consume. `ChainAdapter.hasNetworkFee` decides this, so it is a property of the
  protocol rather than a field an administrator can get wrong.
- Bitcoin's dust limit is a floor no configuration can go below: an output the
  network will not relay cannot be sent at any price.

## Representative Updates

- Only Nano forks have a representative. `ChainAdapter.supportsRepresentative`
  gates the `/update` command, and the frontend hides it for everything else.
- An `UPDATE` queue entry reaching an adapter that has no representatives can
  only be a misconfiguration, so it is discarded rather than retried forever.
- Representative updates move no value, so no balance is at risk when one fails.

## Websocket Push (Nano only)

- `NanoWebSocketService` subscribes to the node's `confirmation` topic, filtered
  to the accounts the bot custodies.
- It is an accelerator, never the authority. Delivery is not guaranteed: the
  node drops notifications under load and a dead socket can go unnoticed. The
  periodic sweep is what makes correctness hold.
- Being subscribed stretches the sweep interval rather than removing it. Losing
  the socket, or dropping a notification, requests an immediate sweep.
- Subscribing with an empty account filter would ask for every confirmation on
  the network, so it waits until the address index is populated.
- A currency with no `websocketUrl` polls only, which is what makes push
  adoptable one currency at a time.

## Collections

| Collection | Purpose |
|---|---|
| `queues` | in-flight sends, receives and representative updates |
| `depositAddresses` | address to user mapping for hot-wallet protocols |
| `depositRecords` | permanent record of credited deposits; the double-credit guard |
| `depositNotices` | deposits already announced as discovered; expires after a week |
| `currencies` | per-currency configuration and chain cursor state |

## Currency Configuration

Chain-relevant fields on a currency document:

- `protocol` - selects the adapter. Absent means `NANO`.
- `nodeUrl` - node RPC. `walletRpcUrl`, `walletRpcUser`, `walletRpcPassword` -
  wallet RPC, withheld from API responses because they gate a spendable wallet.
- `websocketUrl` - Nano push endpoint. Absent means poll only.
- `confirmations` - depth before crediting. Zero or negative is treated as
  unset, since crediting at zero would let an unconfirmed transaction be spent.
- `precision` - decimals in one whole unit. All amounts are stored as integer
  strings in the smallest unit.
- `feeEstimate`, `feePriority` - refreshed from the chain; see Fees above.
- `lastScannedHeight` - deposit scan cursor, written by the adapter.
- `liquidity` - what the hot wallet holds, total rather than spendable, so a
  transient lock does not make a solvent wallet look empty.
- `address` - length-anchored regex the destination is validated against.
- `concealBalances` - withholds amounts from the public audit, for privacy coins.
- `processDeposits`, `processWithdrawals`, `enabled` - independent switches, so a
  currency can be drained, paused or hidden separately.

## Adding a Protocol

1. Implement `ChainAdapter` and give it a protocol constant.
2. Override `supportsRepresentative` and `hasNetworkFee` if the defaults are
   wrong. Both default to the safer answer for a new chain.
3. Insert a currency document naming that protocol.

Nothing else needs to change. The registry discovers adapters through Spring,
prices come from the currency's `priceId`, and the Discord frontend renders
whatever `/currencies` returns.

## Non-Goals

- This document does not cover the internal ledger; see `TRANSFERS.md`.
- This document does not cover node operation or wallet provisioning.
- This document does not define command UX or frontend interaction flow.
