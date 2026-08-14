TTTTT  RRRR    A    N   N   SSS    A    CCCC  TTTTT  I   OOO  N   N   SSS
  T    R   R  A A   NN  N  S      A A  C        T    I  O   O NN  N  S
  T    RRRR  AAAAA  N N N   SSS  AAAAA C        T    I  O   O N N N   SSS
  T    R R   A   A  N  NN      S A   A C        T    I  O   O N  NN      S
  T    R  R  A   A  N   N  SSSS  A   A  CCCC    T    I   OOO  N   N  SSSS

RRRR   EEEEE   QQQ  U   U  EEEEE  SSSS  TTTTT  SSSS 
R   R  E      Q   Q U   U  E     S        T   S    
RRRR   EEEE   Q   Q U   U  EEEE   SSS     T    SSS 
R  R   E      Q  QQ U   U  E         S    T       S 
R   R  EEEEE   QQQQ  UUU   EEEEE SSSS     T   SSSS 

---------------------------------------------------

HEADER
Key=Content-Type
Value=application/json

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/transactions

BODY:
{
     "guildId": "321",
     "userId": "231"
}