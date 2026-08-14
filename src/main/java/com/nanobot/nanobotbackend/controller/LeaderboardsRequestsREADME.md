L      EEEE    A    DDDD   EEEE  RRRR   BBBB   OOO    A   RRRR   DDDD   SSSS 
L      E      A A   D   D  E     R   R  B   B O   O  A A  R   R  D   D S    
L      EEEE  AAAAA  D   D  EEEE  RRRR   BBBB  O   O AAAAA RRRR   D   D  SSS  
L      E     A   A  D   D  E     R  R   B   B O   O A   A R  R   D   D     S 
LLLLL  EEEE  A   A  DDDD   EEEE  R   R  BBBB   OOO  A   A R   R  DDDD  SSSS 

RRRR   EEEEE   QQQ  U   U  EEEEE  SSSS  TTTTT  SSSS 
R   R  E      Q   Q U   U  E     S        T   S    
RRRR   EEEE   Q   Q U   U  EEEE   SSS     T    SSS 
R  R   E      Q  QQ U   U  E         S    T       S 
R   R  EEEEE   QQQQ  UUU   EEEEE SSSS     T   SSSS 

---------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/leaderboards

BODY:
{
     "guildId": "321",
     "userId": "231"
}