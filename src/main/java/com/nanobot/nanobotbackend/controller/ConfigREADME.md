 CCCC   OOO   N   N  FFFFF  III   GGG  
C      O   O  NN  N  F       I   G     
C      O   O  N N N  FFFF    I   G  GG 
C      O   O  N  NN  F       I   G   G 
 CCCC   OOO   N   N  F      III   GGGG 

--------------------------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/requests/config

BODY:
{
     "guildId": "321",
     "userId": "123",
     "fishingLoggingChannelId": "222",
     "fishingChannelId": "213",
     "fishingRole": "111",
     "fishingBypassRoles": ["333"],
     "fishingError": "This is an error message.",
     "fishingFrequency": 15,
     "maximumMinutesActive": 30,
     "maximumActiveUsers": 40,
     "transferLoggingChannelId": "323",
     "aliasData": {
          "guildId": "321",
          "ownerId": "123",
          "singular": "example",
          "plural": "examples",
          "ticker": "XNO",
          "value": "1.23",
          "emoji": "<:example:123>"
     }
}

RESPONSE:
Status=202
Data=ConfigResponseDto