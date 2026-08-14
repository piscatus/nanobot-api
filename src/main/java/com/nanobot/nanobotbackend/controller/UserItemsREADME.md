U   U   SSS   EEEE  RRRR
U   U  S      E     R   R
U   U   SSS   EEEE  RRRR
U   U      S  E     R  R
 UUU   SSSS   EEEE  R   R

III  TTTTT  EEEE  MM    MM   SSSS  
 I     T    E     M M  M M  S     
 I     T    EEEE  M  MM  M   SSS  
 I     T    E     M      M      S 
III    T    EEEE  M      M  SSSS  

-------------------------------------

HEADER:
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/userItems

BODY:
{
"userId": "abc123",
"items": [
        {
            "name": "FISH",
            "quantity": 10    
        },
        {
            "name": "WHALE",
            "quantity": 1    
        }
    ]
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/items/abc123

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/items

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/items?userId=abc123

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/items/651e02160156b6630c3a6586

BODY:
{
    "userId": "abc123",
    "items": [
        {
            "name": "FISH",
            "quantity": 10
        },
        {
            "name": "WHALE",
            "quantity": 2
        },
        {
            "name": "STARFISH",
            "quantity": 0
        },
        {
            "name": "SQUID",
            "quantity": 1
        }
    ]
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/items/651e02160156b6630c3a6586