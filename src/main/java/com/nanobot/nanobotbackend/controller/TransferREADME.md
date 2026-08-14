TTTTT RRRR    A   N   N SSSS  FFFF EEEE RRRR
  T   R   R  A A  NN  N S     F    E    R   R
  T   RRRR  AAAAA N N N  SSS  FFF  EEEE RRRR
  T   R  R  A   A N  NN     S F    E    R  R
  T   R   R A   A N   N SSSS  F    EEEE R   R

---------------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/drop

BODY:
{
     "guildId": "321",
     "channelId": "221",
     "dropId": "333",
     "userId": "123",
     "duration": 0,
     "roleId": "111",
     "users": 10,
     "random": 1,
     "confirmation": false,
     "input": "all fish"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/gift

BODY:
{
     "guildId": "321",
     "channelId": "221",
     "userId": "123",
     "receiverIds: ["123", "231", "321"]
     "confirmation": false,
     "input": ".01 nano"
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/merge

BODY:
{
     "guildId": "321",
     "userId": "123",
     "confirmation": false
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/rain

BODY:
{
     "guildId": "321",
     "channelId": "221",
     "userId": "123",
     "confirmation": false,
     "duration": 0,
     "random": 1,
     "users": 10,
     "input": "all fish",
     "userIdsWithRole: ["123", "231", "321"]
}

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/sell

BODY:
{
     "guildId": "321",
     "userId": "123",
     "confirmation": false,
     "input": "all fish"
}