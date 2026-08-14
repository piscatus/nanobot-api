 GGG   U   U  III  L      DDD   
G      U   U   I   L      D  D  
G  GG  U   U   I   L      D  D  
G   G  U   U   I   L      D  D  
 GGG    UUU   III  LLLLL  DDD   

 CCC   OOO  N   N  FFFFF  III   GGG    U   U  RRRR    A   TTTTT III   OOO  N   N   SSSS
C     O   O NN  N  F       I   G       U   U  R   R  A A    T    I   O   O NN  N  S    
C     O   O N N N  FFF     I   G  GG   U   U  RRRR  AAAAA   T    I   O   O N N N   SSS  
C     O   O N  NN  F       I   G   G   U   U  R  R  A   A   T    I   O   O N  NN      S
 CCC   OOO  N   N  F      III   GGG     UUU   R   R A   A   T   III   OOO  N   N  SSSS 

---------------------------------------------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/guilds

BODY:
{
    "guildId": "626145793413218342",
    "status": "ACTIVE",
    "logChannel": "1095914547740692551",
    "fishingChannel": "957364171215896646",
    "fishingRole": "926034496980152363",
    "fishingError": "You need to vote first",
    "fishingFrequency": "15",
    "maximumMinutesActive": "30",
    "maximumActiveUsers": "40",
    "wallets": [{
	"ticker": "XNO",
	"address": "0",
	"raw": "1000000000000000"
    },{
	"ticker": "BAN",
	"address": "0",
	"raw": "1000000000000000"
    }]
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/guilds

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/guilds?guildId=626145793413218342

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/guilds/652355e3553a1e5d9c8e7aea

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/guilds/652355e3553a1e5d9c8e7aea
{
    "guildId": "626145793413218342",
    "status": "ACTIVE",
    "logChannel": "1095914547740692551",
    "fishingChannel": "957364171215896646",
    "fishingRole": "926034496980152363",
    "fishingError": "You need to vote first",
    "fishingFrequency": "15",
    "maximumMinutesActive": "30",
    "maximumActiveUsers": "40",
    "wallets": [{
	"ticker": "XNO",
	"address": "0",
	"raw": "1000000000000000"
    },{
	"ticker": "BAN",
	"address": "0",
	"raw": "2000000000000000"
    }]
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/guilds/652355e3553a1e5d9c8e7aea
