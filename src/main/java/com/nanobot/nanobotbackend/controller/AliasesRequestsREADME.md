  A    L     III    A     SSSS  EEEEE   SSSS 
 A A   L      I    A A   S      E      S     
AAAAA  L      I   AAAAA   SSS   EEEE    SSS  
A   A  L      I   A   A      S  E          S 
A   A  LLLL  III  A   A  SSSS   EEEEE  SSSS  

RRRR   EEEEE   QQQ  U   U  EEEEE  SSSS  TTTTT  SSSS 
R   R  E      Q   Q U   U  E     S        T   S    
RRRR   EEEE   Q   Q U   U  EEEE   SSS     T    SSS 
R  R   E      Q  QQ U   U  E         S    T       S 
R   R  EEEEE   QQQQ  UUU   EEEEE SSSS     T   SSSS 

----------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/aliases

BODY:
{
     "global": true,
     "guildId": "321",
     "userId": "123"
}

RESPONSE:
Status=200
Data=AliasesResponseDto