U   U  SSS  EEEE  RRRR
U   U S     E     R   R
U   U  SSS  EEEE  RRRR
U   U     S E     R  R
 UUU  SSSS  EEEE  R   R

W   W    A    L      L     EEEE  TTTTT   SSSS 
W W W   A A   L      L     E       T    S    
W W W  AAAAA  L      L     EEEE    T     SSS  
W W W  A   A  L      L     E       T        S 
 W W   A   A  LLLLL  LLLL  EEEE    T    SSSS 

---------------------------------------------

HEADER:
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/userWallets

BODY:
{
    "userId": "43210124",
    "wallets": [
        {
            "ticker": "XNO",
	        "address": "nano_123",
            "raw": "1000000000000000000"    
        },
        {
            "ticker": "BAN",
	        "address": "ban_123",
            "raw": "100000000"    
        }
    ]
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/wallets/626141672761458718

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/wallets

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/wallets?userId=626141672761458718
URL=http://<container>:<port>/wallets?id=123456789

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/wallets/65dbe41b529732107c331b5a
{
    "userId": "626141672761458718",
    "wallets": [
        {
            "ticker": "XNO",
            "raw": "0"    
        },
        {
            "ticker": "BAN",
            "raw": "0"    
        }
    ]
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/wallets/651e04d30156b6630c3a6588