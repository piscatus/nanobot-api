  A     N   N   GGG   L      EEEE  RRRR    SSS 
 A A    NN  N  G      L      E     R   R  S    
AAAAA   N N N  G  GG  L      EEEE  RRRR    SSS  
A   A   N  NN  G   G  L      E     R  R       S 
A   A   N   N   GGG   LLLLL  EEEE  R   R   SSS  

-----------------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/activities

BODY:
{
"guildId": "83746253",
"userId": "43210124",
"timestamp": "2023-10-07T22:20:50.203+00:00"
"ping": false
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/activities

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/anglers?guildId=83746253&userId=43210124&timestamp=2023-10-07T22:20:50.203+00:00

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/anglers?guildId=83746253&userId=43210124

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/anglers?guildId=83746253

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/anglers/651e02160156b6630c3a6586

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/anglers/651e02160156b6630c3a6586

BODY:
{
"guildId": "83746253",
"channelId": "62548757",
"userId": "43210124",
"timestamp": "2024-10-07T23:20:50.203+00:00",
"ticker": "XNO"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/anglers/651e02160156b6630c3a6586

-----------------------------------------------

TICKER

The user's default fishing currency in this guild, set by picking a currency on
/fish and cleared by picking Any. Null means they fish for anything.

This PUT overwrites every field, so omitting ticker clears the user's default.

An angler that only ever set a default has no timestamp and is not resting: it
records a preference, not a catch. A timestamp is what starts a cooldown, and
resting is what queues a reminder, so a preference must not set either.