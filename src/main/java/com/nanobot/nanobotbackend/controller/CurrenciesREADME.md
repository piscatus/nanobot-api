 CCCC   U   U  RRRR   RRRR    EEEEE  N   N  CCCC  III  EEEEE  SSSS  
 C      U   U  R   R  R   R   E      NN  N  C      I   E      S    
 C      U   U  RRRR   RRRR    EEEE   N N N  C      I   EEEE    SSS  
 C      U   U  R   R  R   R   E      N  NN  C      I   E          S 
  CCCC   UUUU  R   R  R   R   EEEEE  N   N  CCCC  III  EEEEE  SSSS

-------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/currencies

BODY:
{
    "ticker": "XNO",
    "name": "Nano",
    "enabled": true,
    "address": "^(nano|xrb)_[13]{1}[13456789abcdefghijkmnopqrstuwxyz]{59}$",
    "nodeUrl": "http://nano-node:7076",
    "emoji": "<:xno:1309539691371954246>",
    "color": "#209ce9",
    "precision": "30",
    "value": "0.632576",
    "processDeposits": true,
    "processWithdrawals": true,
    "minimumDeposit": "1",
    "minimumWithdraw": "1000000000000000000000000",
    "minimumDrop": "1",
    "minimumGift": "1",
    "minimumRain": "1",
    "openDifficulty": "fffffe0000000000",
    "receiveDifficulty": "fffffe0000000000",
    "sendDifficulty": "fffffff800000000",
    "updateDifficulty": "fffffff800000000"
}

NOTES:
- "protocol" selects the chain adapter: NANO, MONERO or BITCOIN. Absent means
  NANO, which is what keeps documents written before Monero existed working.
- The four *Difficulty fields are Nano proof of work and mean nothing elsewhere.
- "confirmations" is the depth before a deposit is credited. Nano uses 1, Monero
  10, Bitcoin 6. Zero or negative is treated as unset.
- "feeEstimate" is refreshed from the chain and is null on feeless networks. It
  is the typical-size floor used for the minimum withdrawal, not the fee shown
  at confirmation. Do not fold it into the minimum; the minimum a user must
  clear is minimumWithdraw + feeEstimate. On /send the API quotes the exact fee
  from the hot wallet when it can.
- "feePriority" is 1 to 4. Monero maps it to transaction priority, Bitcoin to a
  confirmation target.
- "supportsRepresentative" false hides the currency from /update.
- "concealBalances" true withholds amounts from the public audit, for privacy
  coins.
- "lastScannedHeight" and "liquidity" are written by the adapter. Do not set
  them by hand.
- The wallet RPC credentials below are deliberately absent from every response
  body, because they gate a spendable wallet. They can only be set directly in
  the database.

See CHAINS.md for how these fields drive deposits and withdrawals.

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/currencies

BODY (Monero, a fee-bearing privacy coin with no account explorer):
{
    "ticker": "XMR",
    "name": "Monero",
    "protocol": "MONERO",
    "enabled": true,
    "address": "^(?:[48][0-9AB][1-9A-HJ-NP-Za-km-z]{93}|4[0-9AB][1-9A-HJ-NP-Za-km-z]{104})$",
    "nodeUrl": "http://<host>:18081/json_rpc",
    "emoji": "<:xmr:1543841038383321199>",
    "color": "#FF6600",
    "precision": "12",
    "confirmations": "10",
    "feePriority": "1",
    "priceId": "monero",
    "explorerTxUrl": "https://localmonero.co/blocks/search/{value}",
    "concealBalances": true,
    "supportsRepresentative": false,
    "processDeposits": true,
    "processWithdrawals": true,
    "minimumDeposit": "1",
    "minimumWithdraw": "1",
    "minimumDrop": "100",
    "minimumGift": "100",
    "minimumRain": "100"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/currencies

BODY (Bitcoin; note the wallet name in the RPC path, which bitcoind requires
whenever more than one wallet is loaded):
{
    "ticker": "BTC",
    "name": "Bitcoin",
    "protocol": "BITCOIN",
    "enabled": true,
    "address": "^(?:[13][1-9A-HJ-NP-Za-km-z]{25,34}|bc1[qp][qpzry9x8gf2tvdw0s3jn54khce6mua7l]{38,58}|BC1[QP][QPZRY9X8GF2TVDW0S3JN54KHCE6MUA7L]{38,58})$",
    "nodeUrl": "http://<host>:8332/",
    "emoji": "<:btc:1544170255327174756>",
    "color": "#F7931A",
    "precision": "8",
    "confirmations": "6",
    "feePriority": "2",
    "priceId": "bitcoin",
    "explorerTxUrl": "https://www.blockchain.com/explorer/transactions/btc/{value}",
    "explorerAccountUrl": "https://www.blockchain.com/explorer/addresses/btc/{value}",
    "concealBalances": false,
    "supportsRepresentative": false,
    "processDeposits": true,
    "processWithdrawals": true,
    "minimumDeposit": "1",
    "minimumWithdraw": "294",
    "minimumDrop": "1",
    "minimumGift": "1",
    "minimumRain": "1"
}

NOTE: Bitcoin's minimumWithdraw is the P2WPKH dust threshold. An output below it
is non-standard and will not relay, so a lower figure could be advertised but
never honoured.

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/currencies

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/currencies?ticker=XNO
URL=http://<container>:<port>/currencies?ticker=BAN
URL=http://<container>:<port>/currencies?ticker=XMR
URL=http://<container>:<port>/currencies?ticker=BTC

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/currencies/65218b09d9b8457c03071681
URL=http://<container>:<port>/currencies/6521a662d9b8457c03071682

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/currencies/65218b09d9b8457c03071681

BODY:
{
    "ticker": "XNO",
    "name": "Nano",
    "emoji": "<xno:123>",
    "color": "#123456",
    "precision": "30",
    "value": "0.642576",
    "minimumDeposit": "1",
    "minimumWithdraw": "10000000000000000000000",
    "minimumDropDefault": "1000000000000000000000000",
    "minimumGiftDefault": "1000000000000000000000000",
    "minimumRainDefault": "1000000000000000000000000"
}

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/currencies/6521a662d9b8457c03071682

BODY:
{
    "ticker": "BAN",
    "name": "Banano",
    "emoji": "<ban:456>",
    "color": "#654321",
    "precision": "30",
    "value": "0.003986",
    "minimumDeposit": "1",
    "minimumWithdraw": "10000000000000000000000",
    "minimumDropDefault": "1000000000000000000000000",
    "minimumGiftDefault": "1000000000000000000000000",
    "minimumRainDefault": "1000000000000000000000000"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/currencies/65218b09d9b8457c03071681
