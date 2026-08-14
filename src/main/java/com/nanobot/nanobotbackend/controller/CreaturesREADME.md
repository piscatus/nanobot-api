 CCC  RRRR   EEEEE   AAA   TTTTT  U   U  RRRR   EEEEE   SSSS 
C     R   R  E      A   A    T    U   U  R   R  E      S    
C     RRRR   EEEE   AAAAA    T    U   U  RRRR   EEEE    SSS  
C     R  R   E      A   A    T    U   U  R  R   E          S 
 CCC  R   R  EEEEE  A   A    T     UUU   R   R  EEEEE  SSSS  


------------------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/creatures
{
        "name": "Jellyfish",
        "pluralization": "Jellyfishes",
        "capacity": "200",
        "odds": "300",
        "value": "100000000000",
        "ticker": "BAN",
        "emoji": "<:jellyfish:1158820135277183170>",
        "image": "http://discord.gg/img/228973246234872368578"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/creatures

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/creatures/65235c86e8601822a82b487b
{
        "name": "Jellyfish",
        "pluralization": "Jellyfishes",
        "capacity": "200",
        "odds": "300",
        "value": "200000000000",
        "ticker": "BAN",
        "emoji": "<:jellyfish:1158820135277183170>",
        "image": "http://discord.gg/img/228973246234872368578"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/creatures/65235c86e8601822a82b487b