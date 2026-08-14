PPPP   RRRR   OOO   FFFFF   AAA   N   N  IIIII TTTTT IIIII  EEEEE  SSSS 
P   P  R   R O   O  F      A   A  NN  N    I     T     I   E      S         
PPPP   RRRR  O   O  FFFF   AAAAA  N N N    I     T     I   EEEE    SSSS   
P      R  R  O   O  F      A   A  N  NN    I     T     I   E           S 
P      R   R  OOO   F      A   A  N   N  IIIII   T   IIIII  EEEEE  SSSS  

--------------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/profanities

BODY:
{
        "singular": "profanity",
        "plural": "profanities",
        "contains": true
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/profanities

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/profanities?singular=profanity

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/profanities/651e02160156b6630c3a6586

BODY:
{
        "singular": "profanity",
        "plural": "profanities",
        "contains": false
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/aliases/651e02160156b6630c3a6586