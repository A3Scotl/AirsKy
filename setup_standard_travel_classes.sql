-- =============================================
-- STANDARD 3-CLASS TRAVEL SYSTEM SETUP
-- =============================================

-- WARNING: This will remove ALL existing travel classes and create only 3 standard ones
-- Make sure to backup data if needed

-- 1. Clean up existing flight_travel_classes relationships
DELETE FROM flight_travel_classes;

-- 2. Clean up existing seats that reference travel classes
DELETE FROM seats;

-- 3. Remove ALL existing travel classes (clean slate)
DELETE FROM travel_classes;

-- 4. Reset auto increment (optional)
ALTER TABLE travel_classes AUTO_INCREMENT = 1;

-- 5. Insert ONLY 3 standard travel classes
INSERT INTO travel_classes (
    class_name, 
    benefits, 
    price_multiplier, 
    refundable, 
    changeable, 
    cancellation_fee,
    created_at,
    updated_at
) VALUES 
-- Economy Class (1.0x base price)
('Economy', 
 'Standard seating, basic meal service, standard baggage allowance', 
 1.0, 
 true, 
 true, 
 50000.00,
 NOW(),
 NOW()),

-- Business Class (2.0x base price) 
('Business', 
 'Extra legroom, priority boarding, premium meals, increased baggage allowance, lounge access', 
 2.0, 
 true, 
 true, 
 25000.00,
 NOW(),
 NOW()),

-- First Class (3.0x base price)
('First', 
 'Luxury seating, premium service, exclusive lounge access, maximum baggage allowance, personalized service', 
 3.0, 
 true, 
 true, 
 0.00,
 NOW(),
 NOW());

-- =============================================
-- VERIFY SETUP
-- =============================================
SELECT 
    class_id,
    class_name,
    price_multiplier,
    benefits,
    refundable,
    changeable,
    cancellation_fee
FROM travel_classes 
WHERE class_name IN ('Economy', 'Business', 'First')
ORDER BY price_multiplier;