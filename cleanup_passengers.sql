-- Script để clean up orphaned passenger records
-- Xóa các passenger records có seat_id trùng nhau (giữ lại record mới nhất)

USE airsky;

-- Tìm các seat_id bị duplicate
SELECT seat_id, COUNT(*) as count
FROM passengers
WHERE seat_id IS NOT NULL
GROUP BY seat_id
HAVING COUNT(*) > 1;

-- Xóa duplicate records, giữ lại record có id lớn nhất (mới nhất)
DELETE p1 FROM passengers p1
INNER JOIN passengers p2
WHERE p1.seat_id = p2.seat_id
AND p1.passenger_id < p2.passenger_id
AND p1.seat_id IS NOT NULL;

-- Kiểm tra lại sau khi clean up
SELECT seat_id, COUNT(*) as count
FROM passengers
WHERE seat_id IS NOT NULL
GROUP BY seat_id
HAVING COUNT(*) > 1;

-- Hiển thị các passenger records còn lại
SELECT p.passenger_id, p.first_name, p.last_name, p.seat_id, s.seat_number, s.status, b.booking_code, b.status as booking_status
FROM passengers p
LEFT JOIN seats s ON p.seat_id = s.seat_id
LEFT JOIN bookings b ON p.booking_id = b.booking_id
WHERE p.seat_id IS NOT NULL
ORDER BY p.seat_id;