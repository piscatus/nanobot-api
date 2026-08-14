DDDD   RRRR    OOO   PPPP    SSSS
D   D  R   R  O   O  P   P  S   
D   D  RRRR   O   O  PPPP    SSS 
D   D  R  R   O   O  P          S
DDDD   R   R   OOO   P      SSSS 

---------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/drops

BODY:
{
    "dropId": "1158860160329064518",
    "guildId": "626145793413218342",
    "channelId": "924028085475635210",
    "dropperId": "626141672761458718",
    "dropperMessage": "bansplit for friends",
    "dropStartTime": "2023-10-07T23:20:50.203+00:00",
    "dropEndTime": "2023-10-08T23:20:50.203+00:00",
    "ticker": "XNO",
    "value": "1000000000000000000000000",
    "requiredRole": "1000000000000000000000000",
    "maximumEntries": "123000000000000",
    "numberWinners": "10"
}

ENDPOINT:
Method=GET
URL=http://<container>:<port>/drops

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/drops?ticker=XNO
URL=http://<container>:<port>/drops?ticker=BAN

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/drops/6523533d553a1e5d9c8e7ae8

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/drops/6523533d553a1e5d9c8e7ae8

BODY:
{
    "dropId": "1158860160329064518",
    "guildId": "626145793413218342",
    "channelId": "924028085475635210",
    "dropperId": "626141672761458718",
    "dropperMessage": "bansplit for everyone active",
    "dropStartTime": "2023-10-07T23:20:50.203+00:00",
    "dropEndTime": "2023-10-08T23:20:50.203+00:00",
    "ticker": "XNO",
    "value": "1000000000000000000000000",
    "requiredRole": "1000000000000000000000000",
    "maximumEntries": "123000000000000",
    "numberWinners": "100"
}

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/drops/6523533d553a1e5d9c8e7ae8
