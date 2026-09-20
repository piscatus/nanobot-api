TTTTT  RRRR   III  V   V  III    A    DDDD   RRRR    OOO   PPPP
  T    R   R   I   V   V   I    A A   D   D  R   R  O   O  P   P
  T    RRRR    I   V   V   I   AAAAA  D   D  RRRR   O   O  PPPP
  T    R  R    I    V V    I   A   A  D   D  R  R   O   O  P
  T    R   R  III    V    III  A   A  DDDD   R   R   OOO   P

----------------------------------------------------------------

HEADER:
Key=Content-Type
Value=application/json

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/requests/triviadrop

BODY:
{
    "category": "Science",
    "channelId": "123",
    "confirmation": false,
    "difficulty": "medium",
    "duration": 3,
    "seconds": 15,
    "guildId": "231",
    "input": "1 ban",
    "userId": "213",
    "username": "alice",
    "users": 2
}

RESPONSE:
Status=200
Data=TransferResponseDto

A trivia drop is a `/drop` with a question attached: same escrow, same
timer, but only correct answers can win. `random` and `roleId` are forced
off. `users` caps winners rather than joiners (still limited by how finely
the input divides, like `/drop`). Default duration is 3 minutes. `seconds`
is leftover seconds (0–59); a seconds-only request must be at least 10
seconds and does not pick up the 3-minute default. Seconds under 10 are
allowed when `duration` minutes is greater than 0.
`category` and `difficulty` (`easy`, `medium`, or `hard`) are optional
filters for the question; omit either for any.

The chosen question is persisted on `drop.trivia` (`TriviaQuestionDto`):
answers are shuffled (`True` then `False` for boolean questions).
`correctIndex` is JSON write-only so it is stored but never returned to the
bot before the drop ends.

Before the confirmation prompt, the API resolves `category` (case-insensitive
match against enabled categories), validates `difficulty` against `easy`,
`medium` or `hard`, and checks that at least one enabled question matches.
Unknown category: "There are no trivia questions in the category `<typed>`,
sorry! Pick a category from the suggestions." Bad difficulty: "Trivia
difficulty must be one of `easy`, `medium` or `hard`." Empty pool names the
filters that were asked for: no filters → "There are no trivia questions
available right now, sorry!"; category only → "There are no trivia
questions in the category `X`, sorry! Try another category."; difficulty
only → "There are no hard trivia questions, sorry! Try another
difficulty."; both → "There are no hard trivia questions in the category
`X`, sorry! Try another category or difficulty."

The preview `drop.trivia` carries only the requested category and difficulty
so the confirmation embed can show Category, Difficulty (capitalized), and
"Maximum Winners". A question is picked only after confirmation, so a
preview does not consume one. After confirmation the real question is
picked with both filters. It is marked used after the drop exists.
Generated bank rows stay `enabled: false` until a human turns them on.

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/requests/pickup

BODY (trivia drop):
{
    "dropId": "321",
    "userId": "123",
    "userRoles": ["111", "222", "333"],
    "answerIndex": 2
}

`answerIndex` is the 0-based button that was pressed (`trivia:0` ..
`trivia:3`). Omit it on a plain drop so that body is unchanged. On a trivia
drop it is required; the pickup is stored with that index and the ephemeral
reply does not say whether it was right. One answer per user.

The drop ends when the timer runs out or when the `users`-th correct answer
arrives (that pickup sets `endTime` to now). At cleanup, the first `users`
correct answers by timestamp share the reward evenly; if nobody was right
the reward is refunded to the dropper. The end embed reveals the question,
the correct answer, and "Answered: N (M correct)".
