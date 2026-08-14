PPP   III   CCC  K  K  U   U  PPP    SSSS 
P  P   I   C     K K   U   U  P  P  S    
P  P   I   C     KK    U   U  P  P   SSS 
PPP    I   C     K K   U   U  PPP       S 
P     III   CCC  K  K   UUU   P     SSSS 

-----------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/pickups

BODY:
{
"dropId": "83746253",
"userId": "43210124",
"timestamp": "2023-10-07T22:20:50.203+00:00"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/pickups

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/pickups?dropId=83746253&userId=43210124

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/pickups?dropId=83746253

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/pickups/65235aefe8601822a82b487a

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/pickups/65235aefe8601822a82b487a

BODY:
{
"dropId": "83746253",
"userId": "43210124",
"timestamp": "2024-10-07T22:20:50.203+00:00"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/pickups/65235aefe8601822a82b487a