-- 특정 메뉴 삭제
-- 블루베리 요거트 (ID: 34), 플레인 요거트 (ID: 37), 마카롱 (ID: 30) 삭제

DELETE FROM menu WHERE id IN (30, 34, 37);
