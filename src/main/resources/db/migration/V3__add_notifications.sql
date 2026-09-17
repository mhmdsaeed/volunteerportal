CREATE TABLE `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `message` varchar(500) NOT NULL,
  `link` varchar(255) DEFAULT NULL,
  `is_read` bit(1) NOT NULL DEFAULT 0,
  `created_dttm` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_notification_user_id_idx` (`user_id`),
  CONSTRAINT `FK_notification_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
