 CCCC   U   U  RRRR   RRRR    EEEEE  N   N  CCCC  III  EEEEE  SSSS  
 C      U   U  R   R  R   R   E      NN  N  C      I   E      S    
 C      U   U  RRRR   RRRR    EEEE   N N N  C      I   EEEE    SSS  
 C      U   U  R   R  R   R   E      N  NN  C      I   E          S 
  CCCC   UUUU  R   R  R   R   EEEEE  N   N  CCCC  III  EEEEE  SSSS

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
URL=http://<container>:<port>/requests/creatures

BODY:
{
     "guildId": "321",
     "userId": "231"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/currencies

BODY:
{
     "guildId": "321",
     "userId": "231"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/help
{
     "guildId": "321",
     "userId": "231"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/leaderboards
{
     "guildId": "321",
     "userId": "231"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/receive
{
     "guildId": "321",
     "userId": "231"
}