-- Add tip_amount column to orders table
ALTER TABLE orders 
ADD COLUMN tip_amount DECIMAL(19, 2) DEFAULT 0.00 NOT NULL 
COMMENT 'Số tiền tip cho đơn hàng';

-- Update existing rows to have 0 as default tip amount
UPDATE orders 
SET tip_amount = 0.00 
WHERE tip_amount IS NULL;
