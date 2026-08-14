FFFFF  III   SSS  H   H
F       I   S     H   H
FFFF    I    SSS  HHHHH
F       I      S  H   H
F      III  SSS   H   H

-----------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/requests/fish

BODY:
{
    "guildId": "231",
    "userId": "213",
    "userRoles": [ '123', '321', '231' ]
}

RESPONSE:
Status=200
Data=FishResponseDto