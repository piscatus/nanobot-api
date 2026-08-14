V   V  EEEEE RRRR   III  FFFFF III   CCC   A   TTTTT  III   OOO   N   N   SSS 
V   V  E     R   R   I   F      I   C     A A    T     I   O   O  NN  N  S    
V   V  EEEE  RRRR    I   FFF    I   C     AAA    T     I   O   O  N N N   SSS  
 V V   E     R  R    I   F      I   C     A A    T     I   O   O  N  NN      S 
  V    EEEEE R   R  III  F     III   CCC  A A    T    III   OOO   N   N   SSS 

-----------------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/verifications

BODY:
{
  "userId": "43210124",
  "timestamp": "2023-10-07T22:20:50.203+00:00"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/verifications

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/verifications?userId=43210124

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/verifications/651e02160156b6630c3a6586

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/verifications/userId/43210124

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/verifications/651e02160156b6630c3a6586

BODY:
{
  "userId": "43210124",
  "timestamp": "2024-10-07T23:20:50.203+00:00"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/verifications/651e02160156b6630c3a6586
