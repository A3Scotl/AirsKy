-- =====================================================
-- SQL TRIGGERS FOR AUTOMATIC FLIGHT SETUP
-- This script creates triggers to automatically generate:
-- 1. FlightTravelClass entries (3 standard classes)
-- 2. Seats for the flight based on aircraft seatLayout string
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

    -- Create FlightTravelClass entries with automatic pricing (VND)
    INSERT INTO flight_travel_classes (flight_id, travel_class_id, price, capacity, booked_seat)
    VALUES
        (NEW.id, economy_id, NEW.base_price * 1.0, 0, 0),
        (NEW.id, business_id, NEW.base_price * 2.0, 0, 0),
        (NEW.id, first_id, NEW.base_price * 3.0, 0, 0);
END//

-- =====================================================
-- SEAT GENERATION TRIGGER - SIMPLE VERSION
-- Uses aircraft.seatLayout string for seat generation
-- =====================================================

DELIMITER //

DROP TRIGGER IF EXISTS after_flight_insert_seats//
CREATE TRIGGER after_flight_insert_seats
AFTER INSERT ON flights
FOR EACH ROW
BEGIN
    DECLARE aircraft_layout VARCHAR(10);
    DECLARE aircraft_total_seats INT;
    DECLARE layout_sections TEXT;
    DECLARE current_section INT DEFAULT 1;
    DECLARE section_seats INT;
    DECLARE total_sections INT;
    DECLARE current_row INT DEFAULT 1;
    DECLARE seat_index INT DEFAULT 0;
    DECLARE seat_letter CHAR(1);
    DECLARE economy_id INT;
    DECLARE business_id INT;
    DECLARE first_id INT;

    -- Get aircraft layout and total seats
    SELECT seat_layout, total_seats INTO aircraft_layout, aircraft_total_seats
    FROM aircrafts
    WHERE aircraft_id = NEW.aircraft_id;

    -- Get travel class IDs
    SELECT id INTO economy_id FROM travel_classes WHERE class_name = 'Economy' LIMIT 1;
    SELECT id INTO business_id FROM travel_classes WHERE class_name = 'Business' LIMIT 1;
    SELECT id INTO first_id FROM travel_classes WHERE class_name = 'First' LIMIT 1;

    -- Parse layout string (e.g., "3-3" -> sections: 3,3)
    SET layout_sections = REPLACE(aircraft_layout, '-', ',');
    SET total_sections = LENGTH(layout_sections) - LENGTH(REPLACE(layout_sections, ',', '')) + 1;

    -- Calculate rows per class (simple distribution)
    -- First Class: rows 1-2, Business: rows 3-5, Economy: rest
    SET @first_class_rows = 2;
    SET @business_class_rows = 3;
    SET @current_row = 1;

    -- Generate seats for all rows
    seat_generation_loop: WHILE @current_row <= 30 DO -- Assume max 30 rows

        -- Determine travel class for this row
        SET @current_class_id = economy_id; -- Default to economy
        IF @current_row <= @first_class_rows THEN
            SET @current_class_id = first_id;
        ELSEIF @current_row <= (@first_class_rows + @business_class_rows) THEN
            SET @current_class_id = business_id;
        END IF;

        -- Reset seat index for each row
        SET seat_index = 0;
        SET current_section = 1;

        -- Parse each section in the layout
        section_loop: WHILE current_section <= total_sections DO

            -- Get seats for current section
            SET section_seats = CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(layout_sections, ',', current_section), ',', -1) AS UNSIGNED);

            -- Generate seats for this section
            seat_in_section: WHILE seat_index < section_seats DO

                -- Calculate seat letter (A, B, C, D, E, F, etc.)
                SET seat_letter = CHAR(65 + seat_index); -- 65 = ASCII 'A'

                -- Insert seat with appropriate class
                INSERT INTO seats (flight_id, seat_number, travel_class_id, type, status, created_at, updated_at)
                VALUES (
                    NEW.id,
                    CONCAT(@current_row, seat_letter),
                    @current_class_id,
                    'STANDARD',
                    'AVAILABLE',
                    NOW(),
                    NOW()
                );

                SET seat_index = seat_index + 1;
            END WHILE seat_in_section;

            SET current_section = current_section + 1;
        END WHILE section_loop;

        SET @current_row = @current_row + 1;

        -- Safety check: don't create too many seats
        IF (SELECT COUNT(*) FROM seats WHERE flight_id = NEW.id) >= aircraft_total_seats THEN
            LEAVE seat_generation_loop;
        END IF;

    END WHILE seat_generation_loop;

    -- Update flight_travel_classes with actual capacities
    UPDATE flight_travel_classes
    SET capacity = (SELECT COUNT(*) FROM seats WHERE flight_id = NEW.id AND travel_class_id = flight_travel_classes.travel_class_id)
    WHERE flight_id = NEW.id;

END//

DELIMITER ;