  A     CCC  TTTTT  III  V   V  III  TTTTT  III  EEEEE  SSSS 
 A A   C       T     I   V   V   I     T     I   E     S    
AAAAA  C       T     I   V   V   I     T     I   EEEE   SSS  
A   A  C       T     I    V V    I     T     I   E         S 
A   A   CCCC   T    III    V    III    T    III  EEEEE SSSS 

-----------------------------------------------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/activities

BODY:
{
"guildId": "83746253",
"channelId": "62548757",
"userId": "43210124",
"timestamp": "2023-10-07T22:20:50.203+00:00"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/activities

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/activities?guildId=83746253&channelId=62548757&userId=43210124

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/activities?guildId=83746253&channelId=62548757

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/activities/651e02160156b6630c3a6586

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/activities/651e02160156b6630c3a6586

BODY:
{
"guildId": "83746253",
"channelId": "62548757",
"userId": "43210124",
"timestamp": "2023-10-07T23:20:50.203+00:00"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/activities/651e02160156b6630c3a6586

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/activities/updateOrCreate?guildId=83746253&channelId=62548757&userId=43210124