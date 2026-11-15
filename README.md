# Лабораторная работа 2. Простая СУБД на Java

Мини-СУБД с SQL-подобными запросами. Данные хранятся в файлах (папка `data/`), индексы тоже. Нужна Java 8 или новее.

## Сборка и запуск

```
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out dbms.Main
```

Windows (cmd):

```
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
java -cp out dbms.Main
```

Другая папка для данных: `java -cp out dbms.Main my_data`.

## Команды консоли

Запрос заканчивается на `;`, можно писать в несколько строк.

| Команда | Что делает |
|---|---|
| `help` | справка |
| `tables` | список таблиц |
| `desc имя` | столбцы и индексы таблицы |
| `run файл.sql` | выполнить запросы из файла |
| `exit` | выход |

`run demo.sql` выполняет пример. В нём есть запросы, которые специально заканчиваются ошибкой (помечены в файле как ОЖИДАЕМАЯ ОШИБКА).

## Примеры запросов

```sql
CREATE TABLE cars (id INT PRIMARY KEY, brand STRING, price DOUBLE, in_stock BOOL);
CREATE TABLE sales (id INT PRIMARY KEY, car_id INT REFERENCES cars(id), buyer STRING, day DATE);

INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE);
INSERT INTO cars (id, brand) VALUES (2, 'BMW');
SELECT * FROM cars;
SELECT brand, price FROM cars WHERE price > 20000;
UPDATE cars SET price = 30000 WHERE id = 2;
DELETE FROM cars WHERE brand = 'Toyota';

CREATE INDEX idx_cars_brand ON cars (brand);
DROP INDEX idx_cars_brand;
DROP TABLE sales;
```

Типы: `INT`, `DOUBLE`, `STRING`, `DATE` (`'2026-03-14'`), `BOOL`.

## Проверки

```
java -cp out dbms.SelfTest
java -Xmx3g -cp out dbms.Bench
```

`SelfTest` - автоматические проверки, `Bench` - замер скорости `SELECT` с индексом и без.
