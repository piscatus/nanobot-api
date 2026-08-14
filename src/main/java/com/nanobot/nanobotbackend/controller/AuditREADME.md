 AAA   U   U  DDDD   III  TTTTT
A   A  U   U  D   D   I     T  
AAAAA  U   U  D   D   I     T  
A   A  U   U  D   D   I     T  
A   A   UUU   DDDD   III    T  

-------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/audit

BODY:
{
"guildId": "321",
"userId": "123"
}

RESPONSE:
Status=200
Data=AuditResponseDto