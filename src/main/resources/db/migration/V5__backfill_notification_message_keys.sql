-- Give notifications created before V4 a message key + arguments, so they are shown in the
-- viewer's language too. Each statement recognises one of the English sentences the app used to
-- store and extracts its arguments; anything that doesn't match exactly is left as plain text.
-- Arguments are separated by CHAR(31), like MessageArgsConverter (USING utf8mb4: a plain CHAR() is
-- binary, which MySQL won't mix with the column in REGEXP_REPLACE). Matching is case-sensitive ('c').
-- Safe to re-run: only rows without a key are touched.

UPDATE `notification`
SET `message_key` = 'notification.joinApproved',
    `message_args` = REGEXP_REPLACE(`message`, '^Your request to join ''(.*)'' was approved\\.$', '$1', 1, 0, 'c')
WHERE `message_key` IS NULL
  AND REGEXP_LIKE(`message`, '^Your request to join ''.*'' was approved\\.$', 'c');

UPDATE `notification`
SET `message_key` = 'notification.joinRejected',
    `message_args` = REGEXP_REPLACE(`message`, '^Your request to join ''(.*)'' was not approved\\.$', '$1', 1, 0, 'c')
WHERE `message_key` IS NULL
  AND REGEXP_LIKE(`message`, '^Your request to join ''.*'' was not approved\\.$', 'c');

UPDATE `notification`
SET `message_key` = 'notification.joinRequested',
    `message_args` = REGEXP_REPLACE(`message`, '^(\\S+) requested to join ''(.*)''\\.$', CONCAT('$1', CHAR(31 USING utf8mb4), '$2'), 1, 0, 'c')
WHERE `message_key` IS NULL
  AND REGEXP_LIKE(`message`, '^\\S+ requested to join ''.*''\\.$', 'c');

-- "Unranked" was the English placeholder for no grade; it has its own translated message
UPDATE `notification`
SET `message_key` = 'notification.profileUpdatedUnranked',
    `message_args` = REGEXP_REPLACE(`message`, '^Your volunteer profile was updated: grade is now Unranked, points: (\\d+)\\.$', '$1', 1, 0, 'c')
WHERE `message_key` IS NULL
  AND REGEXP_LIKE(`message`, '^Your volunteer profile was updated: grade is now Unranked, points: \\d+\\.$', 'c');

UPDATE `notification`
SET `message_key` = 'notification.profileUpdated',
    `message_args` = REGEXP_REPLACE(`message`, '^Your volunteer profile was updated: grade is now (.*), points: (\\d+)\\.$', CONCAT('$1', CHAR(31 USING utf8mb4), '$2'), 1, 0, 'c')
WHERE `message_key` IS NULL
  AND REGEXP_LIKE(`message`, '^Your volunteer profile was updated: grade is now .*, points: \\d+\\.$', 'c');
