-- Update profile images for menu items
UPDATE menu SET profile = 'https://kitalk.s3.ap-northeast-2.amazonaws.com/menu_1.jpg' 
WHERE id = 1;

UPDATE menu SET profile = 'https://kitalk.s3.ap-northeast-2.amazonaws.com/menu_2.jpg' 
WHERE id = 2;
