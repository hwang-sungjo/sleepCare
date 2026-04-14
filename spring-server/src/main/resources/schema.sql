CREATE TABLE IF NOT EXISTS `user` (
    `user_id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `email` VARCHAR(255) NOT NULL,
    `password` VARCHAR(255) NOT NULL,
    `phone_number` VARCHAR(50),
    `nickname` VARCHAR(50) NOT NULL,
    `profile_image` TEXT,
    `status` VARCHAR(20) DEFAULT 'active'
);
