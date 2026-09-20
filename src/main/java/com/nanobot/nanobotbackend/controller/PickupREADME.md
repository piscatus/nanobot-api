PPPP  III  CCC K   K U   U PPPP 
P   P  I  C    K  K  U   U P   P
PPPP   I  C    KKK   U   U PPPP 
P      I  C    K  K  U   U P    
P     III  CCC K   K  UUU  P    

--------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/pickup

BODY:
{
     "dropId": "321",
     "userId": "123",
     "userRoles": ["111", "222", "333"]
}

On a trivia drop the same endpoint also accepts `answerIndex` (the 0-based
button that was pressed). Omit it on a plain drop.

BODY (trivia drop):
{
     "dropId": "321",
     "userId": "123",
     "userRoles": ["111", "222", "333"],
     "answerIndex": 2
}