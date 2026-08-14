 SSS  EEEEE RRRR   V   V EEEEE RRRR 
S     E     R   R  V   V E     R   R
 SSS  EEEE  RRRR   V   V EEEE  RRRR 
    S E     R  R    V V  E     R  R 
SSSS  EEEEE R   R    V   EEEEE R   R

------------------------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/server

BODY:
{
    "guildId": "321",
    "userId": "231"
}

RESPONSE:
Status=200
Data=ServerResponseDto

---

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/reserves

BODY:
{
     "guildId": "321",
     "userId": "123",
}

RESPONSE:
Status=200
Data=ServerResponseDto