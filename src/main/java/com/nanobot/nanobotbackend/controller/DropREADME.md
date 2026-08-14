DDDD   RRRR    OOO   PPPP
D   D  R   R  O   O  P   P
D   D  RRRR   O   O  PPPP
D   D  R  R   O   O  P
DDDD   R   R   OOO   P

--------------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/requests/drop

BODY:
{
    "channelId": "123",
    "confirmation": false,
    "duration": 30
    "guildId": "231",
    "input": "10 Kraken",
    "random": 1
    "roleId": "222",
    "userId": "213",
    "users": 10,
}

RESPONSE:
Status=200
Data=TransferResponseDto