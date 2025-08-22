-- Add is_popular and profile columns to menu table
ALTER TABLE menu ADD COLUMN is_popular TINYINT(1) DEFAULT 0 NOT NULL;
ALTER TABLE menu ADD COLUMN profile VARCHAR(500) NULL COMMENT 'AWS S3 이미지 URL';

-- Update some existing menus to be popular (예시)
-- 아메리카노, 카페라떼, 바닐라라떼를 인기 메뉴로 설정
UPDATE menu SET is_popular = 1 
WHERE name IN ('아메리카노', '카페라떼', '바닐라라떼', '딸기스무디', '망고스무디');

-- 인덱스 추가 (인기 메뉴 조회 최적화)
CREATE INDEX idx_menu_is_popular ON menu(is_popular);
