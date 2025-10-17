-- =============================================
-- CLEAN UP DUPLICATE TRAVEL CLASSES
-- =============================================

-- 1. First, let's see what duplicates exist
SELECT class_name, COUNT(*) as count
FROM travel_classes
GROUP BY class_name
HAVING COUNT(*) > 1;

-- 2. Keep only one record per class_name (the one with lowest ID)
-- and delete the duplicates
DELETE t1 FROM travel_classes t1
INNER JOIN travel_classes t2
WHERE t1.id > t2.id
AND t1.class_name = t2.class_name;

-- 3. Verify cleanup
SELECT class_name, COUNT(*) as count
FROM travel_classes
GROUP BY class_name
ORDER BY class_name;

-- 4. Show remaining travel classes
SELECT id, class_name, price_multiplier, benefits
FROM travel_classes
ORDER BY price_multiplier;