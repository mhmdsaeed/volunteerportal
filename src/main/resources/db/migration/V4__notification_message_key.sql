-- Notifications are rendered in the viewer's language from a message key + arguments.
-- `message` keeps an English rendering (and is all that older rows have).
ALTER TABLE `notification`
  ADD COLUMN `message_key` varchar(100) DEFAULT NULL AFTER `message`,
  ADD COLUMN `message_args` varchar(1000) DEFAULT NULL AFTER `message_key`;
