TTTTT  RRRR   III  V   V  III    A     SSSS
  T    R   R   I   V   V   I    A A   S
  T    RRRR    I   V   V   I   AAAAA   SSS
  T    R  R    I    V V    I   A   A      S
  T    R   R  III    V    III  A   A  SSSS

-------------------------------------------

HEADER
Key=Content-Type
Value=application/json

The `trivias` collection is the question bank `/triviadrop` draws from.
Generated questions are imported with `enabled` false and are only used once
a human enables them. `hash` is unique: SHA-1 of the normalized question
(trimmed, runs of whitespace collapsed, lower-cased). When `hash` is omitted
on create it is derived that way so API inserts collide with mongoimport.

Answers become Discord button labels, so each answer is capped at 80
characters. A question may have at most 4 answers (1 correct + up to 3
incorrect). `enabled` defaults to false when omitted.

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/trivias

BODY:
{
    "category": "Science",
    "type": "multiple",
    "difficulty": "medium",
    "question": "What is the chemical symbol for gold?",
    "correctAnswer": "Au",
    "incorrectAnswers": ["Ag", "Fe", "Pb"],
    "source": "manual",
    "enabled": false
}

RESPONSE:
Status=201
Data=TriviaEntity

Validation (400) includes:
- category is required
- question is required and at most 1000 characters
- correctAnswer is required
- incorrectAnswers must have at least one answer
- at most 4 answers total
- answers cannot be blank, must be distinct, and each at most 80 characters
- type, when set, must be `multiple` or `boolean`
- difficulty, when set, must be `easy`, `medium` or `hard`

A duplicate normalized question returns 400 "Trivia with the same question already exists".

Omitted defaults on insert: type is `boolean` when there is one distractor and
`multiple` otherwise; difficulty `medium`; source `manual`; enabled false;
hash from the normalized question; timesUsed 0.

---

ENDPOINT:
Method=POST
URL=http://<container>:<port>/trivias/bulk

BODY:
[
    {
        "category": "Science",
        "question": "What is the chemical symbol for gold?",
        "correctAnswer": "Au",
        "incorrectAnswers": ["Ag", "Fe", "Pb"]
    },
    {
        "category": "History",
        "question": "In which year did the Berlin Wall fall?",
        "correctAnswer": "1989",
        "incorrectAnswers": ["1961", "1945", "1991"]
    }
]

RESPONSE:
Status=200
Data=TriviaBulkResultDto
{
    "received": 2,
    "inserted": 1,
    "duplicates": 1,
    "rejected": ["[1] category is required."]
}

`rejected` entries are `"[index] reason"` strings. In-batch duplicate
questions count as duplicates, not rejected.

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/trivias

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/trivias?category=Science
URL=http://<container>:<port>/trivias?enabled=true
URL=http://<container>:<port>/trivias?source=generated

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/trivias/categories

Distinct categories that have at least one enabled question, sorted.

---

ENDPOINT:
Method=GET
URL=http://<container>:<port>/trivias/6523533d553a1e5d9c8e7ae8

---

ENDPOINT:
Method=PUT
URL=http://<container>:<port>/trivias/6523533d553a1e5d9c8e7ae8

BODY:
{
    "category": "Science",
    "type": "multiple",
    "difficulty": "easy",
    "question": "What is the chemical symbol for gold?",
    "correctAnswer": "Au",
    "incorrectAnswers": ["Ag", "Fe", "Pb"],
    "source": "manual",
    "enabled": true
}

RESPONSE:
Status=202
Data=TriviaEntity

The same validation rules as POST apply. `hash` is recomputed from the
normalized question. Setting `enabled` true is how a generated question
enters the live pool.

---

ENDPOINT:
Method=DELETE
URL=http://<container>:<port>/trivias/6523533d553a1e5d9c8e7ae8
