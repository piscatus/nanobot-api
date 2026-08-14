 M   M  EEEEE   SSSS   SSSS   AAA    GGG  EEEEE   SSSS
 MM MM  E      S      S      A   A  G     E      S   
 M M M  EEEE    SSS    SSS   AAAAA  G  GG EEEE    SSS  
 M   M  E          S      S  A   A  G   G E          S 
 M   M  EEEEE  SSSS   SSSS   A   A   GGG  EEEEE  SSSS

------------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/messages

BODY:
{
"userId": "userIdToMessage",
"guildId": "guildIdToEditOrPost",
"channelId": "channelIdToEditOrPost",
"messageId": "messageIdToEdit",
"title": "Here is the title!",
"color": "#FBDD11",
"content": "This is a test message.",
"timestamp": "2023-10-07T23:20:50.203+00:00"
}

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/messages

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/messages/651e02160156b6630c3a6586

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/messages/651e02160156b6630c3a6586

BODY:
{
"userId": "userIdToMessage",
"guildId": "guildIdToEditOrPost"
"channelId": "channelIdToEditOrPost",
"messageId": "messageIdToEdit",
"title": "Here is the edited title!",
"color": "#FBDD11",
"content": "This is an edited test message.",
"timestamp": "2023-10-07T23:20:50.203+00:00"
}

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/messages/651e02160156b6630c3a6586