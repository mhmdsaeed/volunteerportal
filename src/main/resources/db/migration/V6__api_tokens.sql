-- Bearer tokens for the mobile app API (/api/**). Only a SHA-256 hash of each token is stored,
-- so a database leak doesn't reveal usable tokens. A row is deleted on logout (revocation).
CREATE TABLE `api_token` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `token_hash` char(64) NOT NULL,
  `device_name` varchar(100) DEFAULT NULL,
  `created_dttm` datetime NOT NULL,
  `expires_dttm` datetime NOT NULL,
  `last_used_dttm` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_api_token_hash` (`token_hash`),
  KEY `FK_api_token_user_id_idx` (`user_id`),
  CONSTRAINT `FK_api_token_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
