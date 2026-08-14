 SSSS EEEE N   N DDDD 
S     E    NN  N D   D
 SSS  EEEE N N N D   D
    S E    N  NN D   D
SSSS  EEEE N   N DDDD 

----------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/send

BODY:
{
     "guildId": "321",
     "userId": "123",
     "confirmation": true,
     "input": ".01 xno",
     "address": "nano_123"
}

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/update

BODY:
{
     "guildId": "321",
     "userId": "123",
     "confirmation": true,
     "address": "nano_123"
}