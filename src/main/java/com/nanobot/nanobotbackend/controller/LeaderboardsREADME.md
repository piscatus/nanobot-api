L      EEEE    A    DDDD   EEEE  RRRR   BBBB   OOO    A   RRRR   DDDD   SSSS 
L      E      A A   D   D  E     R   R  B   B O   O  A A  R   R  D   D S    
L      EEEE  AAAAA  D   D  EEEE  RRRR   BBBB  O   O AAAAA RRRR   D   D  SSS  
L      E     A   A  D   D  E     R  R   B   B O   O A   A R  R   D   D     S 
LLLLL  EEEE  A   A  DDDD   EEEE  R   R  BBBB   OOO  A   A R   R  DDDD  SSSS 

----------------------------------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/leaderboards

BODY:
{
"guildId": "83746253",
"userId": "43210124",
"items": [
        {
            "name": "FISH",
            "quantity": 1000    
        },
        {
            "name": "WHALE",
            "quantity": 2000    
        }
    ]
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/leaderboards

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/leaderboards?guildId=83746253&userId=43210124

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/leaderboards?guildId=83746253

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/leaderboards?userId=43210124

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/leaderboards/651e02160156b6630c3a6586

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/leaderboards/651e02160156b6630c3a6586

BODY:
{
    "userId": "abc123",
    "items": [
        {
            "name": "FISH",
            "quantity": 10
        },
        {
            "name": "WHALE",
            "quantity": 2
        },
        {
            "name": "STARFISH",
            "quantity": 0
        },
        {
            "name": "SQUID",
            "quantity": 1
        }
    ]
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/leaderboards/651e02160156b6630c3a6586