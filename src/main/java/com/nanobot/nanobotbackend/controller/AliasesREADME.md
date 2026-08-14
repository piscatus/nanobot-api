  A    L     III    A     SSSS  EEEEE   SSSS 
 A A   L      I    A A   S      E      S     
AAAAA  L      I   AAAAA   SSS   EEEE    SSS  
A   A  L      I   A   A      S  E          S 
A   A  LLLL  III  A   A  SSSS   EEEEE  SSSS  

--------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/aliases

BODY:
{
        "guildId": "626145793413218342",
        "singular": "NICE",
        "plural": "NICES",
        "ticker": "XNO",
        "value": "0.0069",
        "emoji": "<a:nice:1158836733530603612>",
        "owner": "626141672761458718"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/aliases

ENDPOINT:
Method=GET
URL=http://<container>:<port>?guildId=626145793413218342&singular=NICE&ticker=XNO

ENDPOINT:
Method=GET
URL=http://<container>:<port>?guildId=626145793413218342&singular=NICE

ENDPOINT:
Method=GET
URL=http://<container>:<port>?guildId=626145793413218342

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/651e02160156b6630c3a6586
{
        "guildId": "626145793413218342",
        "singular": "NICE",
        "plural": "NICES",
        "ticker": "BAN",
        "value": "0.0069",
        "emoji": "<a:nice:1158836733530603612>",
        "owner": "626141672761458718"
}

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/651e02160156b6630c3a6586