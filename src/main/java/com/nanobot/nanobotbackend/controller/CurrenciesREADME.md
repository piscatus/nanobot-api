 CCCC   U   U  RRRR   RRRR    EEEEE  N   N  CCCC  III  EEEEE  SSSS  
 C      U   U  R   R  R   R   E      NN  N  C      I   E      S    
 C      U   U  RRRR   RRRR    EEEE   N N N  C      I   EEEE    SSS  
 C      U   U  R   R  R   R   E      N  NN  C      I   E          S 
  CCCC   UUUU  R   R  R   R   EEEEE  N   N  CCCC  III  EEEEE  SSSS

-------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/currencies

BODY:
{
    "ticker": "XNO",
    "name": "Nano",
    "enabled": true,
    "address": "^(nano|xrb)_[13]{1}[13456789abcdefghijkmnopqrstuwxyz]{59}$",
    "nodeUrl": "http://nano-node:7076",
    "emoji": "<:xno:1309539691371954246>",
    "color": "#209ce9",
    "precision": "30",
    "value": "0.632576",
    "processDeposits": true,
    "processWithdrawals": true,
    "minimumDeposit": "1",
    "minimumWithdraw": "1000000000000000000000000",
    "minimumDrop": "1",
    "minimumGift": "1",
    "minimumRain": "1",
    "openDifficulty": "fffffe0000000000",
    "receiveDifficulty": "fffffe0000000000",
    "sendDifficulty": "fffffff800000000",
    "updateDifficulty": "fffffff800000000"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/currencies

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/currencies?ticker=XNO
URL=http://<container>:<port>/currencies?ticker=BAN

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/currencies/65218b09d9b8457c03071681
URL=http://<container>:<port>/currencies/6521a662d9b8457c03071682

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/currencies/65218b09d9b8457c03071681

BODY:
{
    "ticker": "XNO",
    "name": "Nano",
    "emoji": "<xno:123>",
    "color": "#123456",
    "precision": "30",
    "value": "0.642576",
    "minimumDeposit": "1",
    "minimumWithdraw": "10000000000000000000000",
    "minimumDropDefault": "1000000000000000000000000",
    "minimumGiftDefault": "1000000000000000000000000",
    "minimumRainDefault": "1000000000000000000000000"
}

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/currencies/6521a662d9b8457c03071682

BODY:
{
    "ticker": "BAN",
    "name": "Banano",
    "emoji": "<ban:456>",
    "color": "#654321",
    "precision": "30",
    "value": "0.003986",
    "minimumDeposit": "1",
    "minimumWithdraw": "10000000000000000000000",
    "minimumDropDefault": "1000000000000000000000000",
    "minimumGiftDefault": "1000000000000000000000000",
    "minimumRainDefault": "1000000000000000000000000"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/currencies/65218b09d9b8457c03071681
