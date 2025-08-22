-- Rename is_popular column to popular
ALTER TABLE menu CHANGE COLUMN is_popular popular TINYINT(1) DEFAULT 0 NOT NULL;

-- Drop old index
DROP INDEX idx_menu_is_popular ON menu;

-- Create new index with updated column name
CREATE INDEX idx_menu_popular ON menu(popular);
