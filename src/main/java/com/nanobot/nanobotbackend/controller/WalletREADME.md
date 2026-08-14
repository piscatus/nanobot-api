W   W   AAA  L     L      EEEEE TTTTT
W   W  A   A L     L      E       T  
W W W  AAAAA L     L      EEEE    T  
WW WW  A   A L     L      E       T  
W   W  A   A LLLLL LLLLL  EEEEE   T  

-------------------------------------

HEADER:
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/wallet

BODY:
{
    "guildId": "321",
    "userId": "231"
}

RESPONSE:
Status=200
Data=UserWalletsResponseDto