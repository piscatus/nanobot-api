 QQQQ   U   U   EEEE   U   U   EEEE   SSSS 
Q    Q  U   U   E      U   U   E     S    
Q    Q  U   U   EEE    U   U   EEE    SSS  
Q   QQ  U   U   E      U   U   E         S 
 QQQQ    UUU    EEEE    UUU    EEEE  SSSS 

------------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/queues

BODY:
{
"userId": "43210124",
"address": "nano_1234567890",
"xid": "1BC689237F73DCA7264",
"raw": "11100000000000000000000000",
"ticker": "XNO",
"timestamp": "2023-10-07T23:20:50.203+00:00"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/queues

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/queues/651e02160156b6630c3a6586

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/queues/651e02160156b6630c3a6586
{
"userId": "43210124",
"address": "nano_1234567890",
"xid": "1BC689237F73DCA7264",
"raw": "11100000000000000000000000",
"ticker": "BAN",
"timestamp": "2023-10-07T23:20:50.203+00:00"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/queues/651e02160156b6630c3a6586