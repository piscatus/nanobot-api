  EEEEE  M   M   OOO     JJJ  III   SSS 
  E      MM MM  O   O      J   I   S    
  EEEE   M M M  O   O  J   J   I    SSS 
  E      M   M  O   O  J   J   I       S 
  EEEEE  M   M   OOO    JJJ   III   SSS 

----------------------------------------

HEADER
Key=Content-Type
Value=application/json

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/emojis

BODY:
{
  "category": "reactions",
  "name": "thumbs_up",
  "emoji": "👍"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/emojis

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/emojis?category=reactions&name=thumbs_up

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/emojis?category=reactions

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/emojis?name=thumbs_up

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/emojis/651e02160156b6630c3a6586

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/emojis/category/reactions/name/thumbs_up

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/emojis/651e02160156b6630c3a6586

BODY:
{
  "category": "reactions",
  "name": "thumbs_up",
  "emoji": "👍"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/emojis/651e02160156b6630c3a6586
