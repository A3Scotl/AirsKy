-- =====================================================
-- SQL TRIGGERS FOR AUTOMATIC FLIGHT SETUP
-- This script creates triggers to automatically generate:
-- 1. FlightTravelClass entries (3 standard classes)
-- 2. Seats for the flight based on aircraft capacity
-- =====================================================

DELIMITER //

-- Drop existing triggers if they exist
DROP TRIGGER IF EXISTS after_flight_insert//
DROP TRIGGER IF EXISTS after_flight_insert_seats//

-- Trigger 1: Auto-create FlightTravelClass when Flight is created
CREATE TRIGGER after_flight_insert
AFTER INSERT ON flights
FOR EACH ROW
BEGIN
    DECLARE economy_id INT;
    DECLARE business_id INT;
    DECLARE first_id INT;
    
    -- Get the IDs of the 3 standard travel classes
    SELECT id INTO economy_id FROM travel_classes WHERE class_name = 'Economy' LIMIT 1;
    SELECT id INTO business_id FROM travel_classes WHERE class_name = 'Business' LIMIT 1;
    SELECT id INTO first_id FROM travel_classes WHERE class_name = 'First' LIMIT 1;
    
    -- Create FlightTravelClass entries with automatic pricing
    INSERT INTO flight_travel_classes (flight_id, travel_class_id, price, capacity, booked_seat)
    VALUES 
        (NEW.id, economy_id, NEW.base_price * 1.0, 0, 0),
        (NEW.id, business_id, NEW.base_price * 2.0, 0, 0),
        (NEW.id, first_id, NEW.base_price * 3.0, 0, 0);
END//

-- Trigger 2: Auto-create Seats when Flight is created
CREATE TRIGGER after_flight_insert_seats
AFTER INSERT ON flights
FOR EACH ROW
BEGIN
    DECLARE aircraft_capacity INT;
    DECLARE economy_id INT;
    DECLARE business_id INT;
    DECLARE first_id INT;
    DECLARE first_seats INT;
    DECLARE business_seats INT;
    DECLARE economy_seats INT;
    DECLARE seat_counter INT DEFAULT 1;
    
    -- Get aircraft capacity
    SELECT total_seats INTO aircraft_capacity 
    FROM aircrafts 
    WHERE aircraft_id = NEW.aircraft_id;
    
    -- Get travel class IDs
    SELECT id INTO economy_id FROM travel_classes WHERE class_name = 'Economy' LIMIT 1;
    SELECT id INTO business_id FROM travel_classes WHERE class_name = 'Business' LIMIT 1;
    SELECT id INTO first_id FROM travel_classes WHERE class_name = 'First' LIMIT 1;
    
    -- Calculate seat distribution (First 10%, Business 20%, Economy 70%)
    SET first_seats = CEIL(aircraft_capacity * 0.10);
    SET business_seats = CEIL(aircraft_capacity * 0.20);
    SET economy_seats = aircraft_capacity - first_seats - business_seats;
    
    -- Create First Class seats (Rows 1-2)
    WHILE seat_counter <= first_seats DO
        INSERT INTO seats (seat_number, flight_id, travel_class_id, status)
        VALUES (
            CONCAT(CEILING(seat_counter / 6), CHAR(65 + ((seat_counter - 1) % 6))),
            NEW.id,
            first_id,
            'AVAILABLE'
        );
        SET seat_counter = seat_counter + 1;
    END WHILE;
    
    -- Create Business Class seats (Following rows)
    SET seat_counter = 1;
    WHILE seat_counter <= business_seats DO
        INSERT INTO seat (seat_number, flight_id, travel_class_id, status)
        VALUES (
            CONCAT(CEILING((first_seats + seat_counter) / 6), CHAR(65 + ((first_seats + seat_counter - 1) % 6))),
            NEW.id,
            business_id,
            'AVAILABLE'
        );
        SET seat_counter = seat_counter + 1;
    END WHILE;
    
    -- Create Economy Class seats (Remaining rows)
    SET seat_counter = 1;
    WHILE seat_counter <= economy_seats DO
        INSERT INTO seat (seat_number, flight_id, travel_class_id, status)
        VALUES (
            CONCAT(CEILING((first_seats + business_seats + seat_counter) / 6), CHAR(65 + ((first_seats + business_seats + seat_counter - 1) % 6))),
            NEW.id,
            economy_id,
            'AVAILABLE'
        );
        SET seat_counter = seat_counter + 1;
    END WHILE;
    
    -- Update FlightTravelClass capacity counts
    UPDATE flight_travel_class 
    SET capacity = first_seats 
    WHERE flight_id = NEW.id AND travel_class_id = first_id;
    
    UPDATE flight_travel_class 
    SET capacity = business_seats 
    WHERE flight_id = NEW.id AND travel_class_id = business_id;
    
    UPDATE flight_travel_class 
    SET capacity = economy_seats 
    WHERE flight_id = NEW.id AND travel_class_id = economy_id;
END//

DELIMITER ;

-- =====================================================
-- TEST THE TRIGGERS
-- =====================================================

-- Test query to verify trigger works
-- INSERT INTO flight (flight_code, airline_id, aircraft_id, departure_airport_id, 
--                    arrival_airport_id, departure_gate_id, arrival_gate_id, 
--                    departure_time, arrival_time, flight_duration, base_price, status)
-- VALUES ('TEST001', 1, 1, 1, 2, 1, 2, 
--         '2024-01-15 08:00:00', '2024-01-15 10:30:00', 150, 1000000, 'SCHEDULED');

-- Verify results:
-- SELECT * FROM flight_travel_class WHERE flight_id = LAST_INSERT_ID();
-- SELECT COUNT(*) as seat_count, travel_class_id FROM seat WHERE flight_id = LAST_INSERT_ID() GROUP BY travel_class_id;