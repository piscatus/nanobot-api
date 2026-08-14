  CCCC   OOO   M   M  M   M   AA   N   N  DDDD    SSS
 C      O   O  MM MM  MM MM  A  A  NN  N  D   D  S
 C      O   O  M M M  M M M  AAAA  N N N  D   D   SSS
 C      O   O  M   M  M   M  A  A  N  NN  D   D      S
  CCCC   OOO   M   M  M   M  A  A  N   N  DDDD    SSS

------------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/commands
{
    "name": "rain",
    "commandId": "123",
    "status": "ACTIVE"
}

ENDPOINT:
Method=GET
URL=http://<container>:<port>/commands

ENDPOINT:
Method=GET
URL=http://<container>:<port>/commands?name=rain

ENDPOINT:
Method=GET
URL=http://<container>:<port>/commands/65218b09d9b8457c03071681

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/commands/65218b09d9b8457c03071681
{
    "name": "rain",
    "commandId": "123",
    "status": "LOCKED"
}

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/commands/65218b09d9b8457c03071681
