-- =============================================
-- QUICK DATABASE SETUP FOR AUTOMATED FLIGHTS
-- =============================================

-- 1. Clean existing data
DELETE FROM flight_travel_classes;
DELETE FROM seats;  
DELETE FROM travel_classes;

-- 2. Create 3 standard travel classes
INSERT INTO travel_classes (class_name, benefits, price_multiplier, refundable, changeable, cancellation_fee) 
VALUES 
('Economy', 'Standard seating, basic meal service', 1.0, true, true, 50000.00),
('Business', 'Extra legroom, priority boarding, premium meals', 2.0, true, true, 25000.00),
('First', 'Luxury seating, premium service, exclusive lounge access', 3.0, true, true, 0.00);

-- 3. Verify setup
SELECT id, class_name, price_multiplier FROM travel_classes ORDER BY price_multiplier;