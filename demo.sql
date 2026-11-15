-- таблицы
CREATE TABLE cars (id INT PRIMARY KEY, brand STRING, price DOUBLE, in_stock BOOL);
CREATE TABLE sales (id INT PRIMARY KEY, car_id INT REFERENCES cars(id), buyer STRING, day DATE);

-- машины
INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE);
INSERT INTO cars (id, brand) VALUES (2, 'BMW');
INSERT INTO cars VALUES (3, 'Audi', 41000.5, TRUE), (4, 'Toyota', 18000.0, FALSE), (5, 'Kia', 15500.0, TRUE);
INSERT INTO cars VALUES (1, 'Lada', 9000.0, TRUE);

-- продажи
INSERT INTO sales VALUES (1, 1, 'Ivanov', '2026-03-14'), (2, 3, 'Petrov', '2026-04-02');
INSERT INTO sales VALUES (3, 99, 'Sidorov', '2026-04-10');

SELECT * FROM cars;
SELECT brand, price FROM cars WHERE price > 20000;
SELECT * FROM cars WHERE brand = 'Toyota' AND in_stock = TRUE;
SELECT * FROM cars WHERE price IS NULL OR brand = 'Kia';
SELECT * FROM sales WHERE day >= '2026-04-01';

UPDATE cars SET price = 30000 WHERE id = 2;
UPDATE cars SET price = 17000.0, in_stock = TRUE WHERE brand = 'Toyota' AND id > 1;
SELECT * FROM cars;

DELETE FROM cars WHERE brand = 'Toyota';
DELETE FROM sales WHERE id = 1;
DELETE FROM cars WHERE brand = 'Toyota';
SELECT * FROM cars;

CREATE INDEX idx_cars_brand ON cars (brand);
SELECT * FROM cars WHERE brand = 'Audi';
SELECT * FROM cars WHERE price > 16000;
DROP TABLE cars;
