 AAA    CCCC  TTTTT  III  V   V  EEEEE
A   A  C        T     I   V   V  E    
AAAAA  C        T     I   V   V  EEEE 
A   A  C        T     I    V V   E    
A   A   CCCC    T    III    V    EEEEE

--------------------------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/active

BODY:
{
     "channelId": "123",
     "duration": 60,
     "guildId": "321",
     "random": 0,
     "userId": "231",
     "userIdsWithRole": [ '0' ],
     "users": 0
}

RESPONSE:
Status=200
Data=ActiveResponseDto