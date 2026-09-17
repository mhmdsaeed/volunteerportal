-- ==========================================================================
-- Drop in child-to-parent order (fresh-install migration)
-- ==========================================================================
DROP TABLE IF EXISTS `attend`;
DROP TABLE IF EXISTS `event`;
DROP TABLE IF EXISTS `volunteer_initiative_answer`;
DROP TABLE IF EXISTS `volunteer_initiative`;
DROP TABLE IF EXISTS `initiative_question`;
DROP TABLE IF EXISTS `initiative`;
DROP TABLE IF EXISTS `office`;
DROP TABLE IF EXISTS `question_lib`;
DROP TABLE IF EXISTS `question_lib_cat`;
DROP TABLE IF EXISTS `volunteer_profile`;
DROP TABLE IF EXISTS `grade`;
DROP TABLE IF EXISTS `user_roles`;
DROP TABLE IF EXISTS `users`;
DROP TABLE IF EXISTS `roles`;
DROP TABLE IF EXISTS `configset`;

-- ==========================================================================
-- Config / lookup
-- ==========================================================================
CREATE TABLE `configset` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `configset_key` varchar(255) NOT NULL,
  `configset_value` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `configset_key_UNIQUE` (`configset_key`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ==========================================================================
-- Auth: roles / users / user_roles
-- ==========================================================================
CREATE TABLE `roles` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `registration_dttm` datetime DEFAULT NULL,
  `enabled` bit(1) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username_UNIQUE` (`username`),
  UNIQUE KEY `email_UNIQUE` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `user_roles` (
  `user_id` bigint NOT NULL,
  `role_id` bigint NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  KEY `INDX_USER_ROLES_ROLE_ID_idx` (`role_id`),
  CONSTRAINT `FK_USER_ROLES_ROLE_ID` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FK_USER_ROLES_USER_ID` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ==========================================================================
-- Volunteer profile
-- ==========================================================================
CREATE TABLE `grade` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `volunteer_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `first_name` varchar(255) DEFAULT NULL,
  `last_name` varchar(255) DEFAULT NULL,
  `mobile` varchar(255) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `address` varchar(255) DEFAULT NULL,
  `points` bigint DEFAULT 0,
  `grade_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id_UNIQUE` (`user_id`),
  KEY `FK_volunteer_profile_grade_id_idx` (`grade_id`),
  CONSTRAINT `FK_volunteer_profile_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
  CONSTRAINT `FK_volunteer_profile_grade_id` FOREIGN KEY (`grade_id`) REFERENCES `grade` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ==========================================================================
-- Question library (reusable question bank)
-- ==========================================================================
CREATE TABLE `question_lib_cat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `question_lib` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_text` varchar(255) DEFAULT NULL,
  `question_type` int DEFAULT NULL,           /* 1: true_false, 2: one_of_n, 3: multi_of_n, 4: free_text */
  `question_choices_count` int DEFAULT NULL,  /* 1:2, 2:n, 3:n, 4:0 */
  `question_choices` varchar(255) DEFAULT NULL,
  `question_lib_cat_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_question_lib_cat_id_idx` (`question_lib_cat_id`),
  CONSTRAINT `FK_question_lib_cat_id` FOREIGN KEY (`question_lib_cat_id`) REFERENCES `question_lib_cat` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ==========================================================================
-- Organization: office / initiative / initiative_question
-- ==========================================================================
CREATE TABLE `office` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `role_id` bigint DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_role_id_idx` (`role_id`),
  KEY `FK_office_user_id_idx` (`user_id`),
  CONSTRAINT `FK_office_role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`),
  CONSTRAINT `FK_office_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `initiative` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `description` varchar(500) DEFAULT NULL,
  `office_id` bigint DEFAULT NULL,
  `supervisor_id` bigint DEFAULT NULL,
  `question_count` int DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_initiative_office_id_idx` (`office_id`),
  KEY `FK_initiative_supervisor_id_idx` (`supervisor_id`),
  CONSTRAINT `FK_initiative_office_id` FOREIGN KEY (`office_id`) REFERENCES `office` (`id`),
  CONSTRAINT `FK_initiative_supervisor_id` FOREIGN KEY (`supervisor_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `initiative_question` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `question_text` varchar(255) DEFAULT NULL,
  `question_type_id` int DEFAULT NULL,
  `question_choices_count` int DEFAULT NULL,
  `question_choices_text` varchar(255) DEFAULT NULL,
  `initiative_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_initiative_id_idx` (`initiative_id`),
  CONSTRAINT `FK_initiative_question_initiative_id` FOREIGN KEY (`initiative_id`) REFERENCES `initiative` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ==========================================================================
-- Volunteer participation
-- ==========================================================================
CREATE TABLE `volunteer_initiative` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_join_dttm` datetime DEFAULT NULL,
  `response_join_dttm` datetime DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `initiative_id` bigint DEFAULT NULL,
  `answer_count` int DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_volunteer_initiative_user_id_idx` (`user_id`),
  KEY `FK_volunteer_initiative_initiative_id_idx` (`initiative_id`),
  CONSTRAINT `FK_volunteer_initiative_initiative_id` FOREIGN KEY (`initiative_id`) REFERENCES `initiative` (`id`),
  CONSTRAINT `FK_volunteer_initiative_user_id` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `volunteer_initiative_answer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `answer_text` varchar(255) DEFAULT NULL,
  `answer_dttm` datetime DEFAULT NULL,
  `answer_choice_number` int DEFAULT NULL,
  `volunteer_initiative_id` bigint DEFAULT NULL,
  `initiative_question_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_volunteer_initiative_answer_volunteer_initiative_id_idx` (`volunteer_initiative_id`),
  KEY `FK_volunteer_initiative_answer_initiative_question_id_idx` (`initiative_question_id`),
  CONSTRAINT `FK_volunteer_initiative_answer_initiative_question_id` FOREIGN KEY (`initiative_question_id`) REFERENCES `initiative_question` (`id`),
  CONSTRAINT `FK_volunteer_initiative_answer_volunteer_initiative_id` FOREIGN KEY (`volunteer_initiative_id`) REFERENCES `volunteer_initiative` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- ==========================================================================
-- Events / attendance
-- ==========================================================================
CREATE TABLE `event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `from_dttm` datetime DEFAULT NULL,
  `to_dttm` datetime DEFAULT NULL,
  `loc_longitude` double DEFAULT NULL,
  `loc_latitude` double DEFAULT NULL,
  `loc_url` varchar(255) DEFAULT NULL,
  `initiative_id` bigint DEFAULT NULL,
  `enabled` bit(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_event_initiative_id_idx` (`initiative_id`),
  CONSTRAINT `FK_event_initiative_id` FOREIGN KEY (`initiative_id`) REFERENCES `initiative` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `attend` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `attend_in_out` int DEFAULT NULL,
  `attend_dttm` datetime DEFAULT NULL,
  `note` varchar(255) DEFAULT NULL,
  `volunteer_initiative_id` bigint DEFAULT NULL,
  `event_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK_attend_event_id_idx` (`event_id`),
  KEY `FK_attend_volunteer_initiative_id_idx` (`volunteer_initiative_id`),
  CONSTRAINT `FK_attend_event_id` FOREIGN KEY (`event_id`) REFERENCES `event` (`id`),
  CONSTRAINT `FK_attend_volunteer_initiative_id` FOREIGN KEY (`volunteer_initiative_id`) REFERENCES `volunteer_initiative` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
