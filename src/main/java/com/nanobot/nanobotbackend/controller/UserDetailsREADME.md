U   U   SSS  EEEEE  RRRR 
U   U  S     E      R   R
U   U   SSS  EEEE   RRRR
U   U      S E      R  R
 UUU   SSSS  EEEEE  R   R

DDDD   EEEEE  TTTTT   A    III  L      SSSS
D   D  E        T    A A    I   L     S    
D   D  EEEE     T   AAAAA   I   L      SSS  
D   D  E        T   A   A   I   L         S
DDDD   EEEEE    T   A   A  III  LLLL  SSSS 

-------------------------------------------

HEADER:
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/users

BODY:
{
    "status": "ACTIVE",
    "seed": "E1CB58DCB23",
    "userId": "43210123",
    "subordinateUserId": "9964246"
}

NOTE: "seed" derives Nano-family deposit addresses only. Monero and Bitcoin
allocate an address from a hot wallet instead and store the mapping in
depositAddresses, so a user with no seed can still hold and receive those. See
CHAINS.md.

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/users/626141672761458718

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/users

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/users?userId=626141672761458718

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/users/active
URL=http://<container>:<port>/users/locked
URL=http://<container>:<port>/users/banned

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/users/65ce8f20006ccd61fc736760

BODY:
{
    "status": "BANNED",
    "seed": "E1CB58DCB23",
    "userId": "43210123",
    "subordinateUserId": "9964246"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/users/651dfb260156b6630c3a6585

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/users/getOrCreate/abc123