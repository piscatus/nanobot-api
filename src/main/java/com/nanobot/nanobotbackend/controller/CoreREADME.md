 CCCC  OOO  RRRR  EEEE
C     O   O R   R E   
C     O   O RRRR  EEEE
C     O   O R  R  E   
 CCCC  OOO  R   R EEEE

----------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/bonuses
{
     "guildId": "321",
     "userId": "123"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/cute
{
     "guildId": "321",
     "userId": "123"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/roles
{
     "guildId": "321",
     "userId": "123"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/rules
{
     "guildId": "321",
     "userId": "123"
}