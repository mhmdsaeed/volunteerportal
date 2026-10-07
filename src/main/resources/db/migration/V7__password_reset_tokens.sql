-- One-time links for "Forgot your password?" (emailed to the user). Only a SHA-256 hash of each
-- token is stored, as for api_token. A row is deleted when it is used, when a newer link is
-- requested, or when the user's password changes another way.
CREATE TABLE `password_reset_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` char(64) NOT NULL,
  `created_dttm` datetime NOT NULL,
  `expires_dttm` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_password_reset_token_hash` (`token_hash`),
  KEY `FK_password_reset_token_user_id_idx` (`user_id`),
  CONSTRAINT `FK_password_reset_token_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
