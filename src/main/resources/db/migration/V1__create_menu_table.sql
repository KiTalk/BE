CREATE TABLE IF NOT EXISTS menu (
                                     id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
                                     name VARCHAR(120) NOT NULL,
                                     temperature ENUM('hot','ice','none') NOT NULL DEFAULT 'none',
                                     price INT UNSIGNED NOT NULL,
                                     category ENUM('커피','스무디','버블티','주스','디저트','기타 음료','스페셜 티','에이드','차','프라페','특색 라떼') NOT NULL,
                                     is_active TINYINT(1) NOT NULL DEFAULT 1,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (id),
                                     UNIQUE KEY uk_menus_name_temp (name, temperature)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
