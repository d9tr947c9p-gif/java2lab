package dbms;

import dbms.db.Database;
import dbms.db.DbException;
import dbms.db.Result;
import dbms.db.Table;
import dbms.sql.Parser;
import dbms.store.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Простая проверка работы без JUnit: запускается обычным main. */
public class SelfTest {
    private static int ok = 0, bad = 0;

    private static void check(boolean c, String name) {
        if (c) ok++;
        else bad++;
        System.out.println((c ? "OK    " : "FAIL  ") + name);
    }

    private static Result q(Database db, String sql) {
        return Parser.parse(sql).run(db);
    }

    private static boolean fails(Database db, String sql) {
        try {
            q(db, sql);
            return false;
        } catch (DbException e) {
            return true;
        }
    }

    private static String rows(Database db, String sql) {
        return Arrays.deepToString(q(db, sql).getRows().toArray());
    }

    public static void main(String[] args) throws IOException {
        Path dir = Files.createTempDirectory("dbtest");
        Database db = new Database(new FileRepository(dir.toString()));

        q(db, "CREATE TABLE cars (id INT PRIMARY KEY, brand STRING, price DOUBLE, in_stock BOOL)");
        q(db, "CREATE TABLE sales (id INT PRIMARY KEY, car_id INT REFERENCES cars(id), buyer STRING, day DATE)");
        check(db.tables().size() == 2, "две таблицы созданы");
        check(fails(db, "CREATE TABLE cars (id INT PRIMARY KEY)"), "повторное создание таблицы");
        check(fails(db, "CREATE TABLE x (a INT, b INT)"), "таблица без PRIMARY KEY");
        check(fails(db, "CREATE TABLE y (a INT PRIMARY KEY, b INT REFERENCES nope)"), "ссылка на несуществующую таблицу");

        q(db, "INSERT INTO cars VALUES (1, 'Toyota', 25000.0, TRUE)");
        q(db, "INSERT INTO cars (id, brand) VALUES (2, 'BMW')");
        q(db, "INSERT INTO cars VALUES (3, 'Audi', 41000.5, TRUE), (4, 'Toyota', 18000, FALSE), (5, 'Kia', 15500.0, TRUE)");
        check(db.table("cars").size() == 5, "вставка одной и нескольких строк");
        check(fails(db, "INSERT INTO cars VALUES (1, 'Lada', 1.0, TRUE)"), "дубликат первичного ключа");
        check(fails(db, "INSERT INTO cars VALUES (10, 'A', 1.0, TRUE), (10, 'B', 1.0, TRUE)"), "дубликат внутри одного INSERT");
        check(db.table("cars").size() == 5, "после ошибки ничего не вставилось");
        check(fails(db, "INSERT INTO cars (brand) VALUES ('NoId')"), "пустой первичный ключ");
        check(fails(db, "INSERT INTO cars VALUES ('abc', 'A', 1.0, TRUE)"), "строка в INT");
        check(fails(db, "INSERT INTO cars VALUES (11, 'A', 1.0)"), "не хватает значений");

        q(db, "INSERT INTO sales VALUES (1, 1, 'Ivanov', '2026-03-14'), (2, 3, 'Petrov', '2026-04-02')");
        check(fails(db, "INSERT INTO sales VALUES (3, 99, 'X', '2026-04-10')"), "внешний ключ: нет такой машины");
        check(fails(db, "INSERT INTO sales VALUES (3, 1, 'X', '2026-13-45')"), "неверная дата");

        check(q(db, "SELECT * FROM cars").getRows().size() == 5, "SELECT *");
        check(rows(db, "SELECT brand, price FROM cars WHERE price > 20000").equals("[[Toyota, 25000.0], [Audi, 41000.5]]"), "SELECT столбцы + WHERE");
        check(q(db, "SELECT * FROM cars WHERE brand = 'Toyota' AND in_stock = TRUE").getRows().size() == 1, "AND");
        check(q(db, "SELECT * FROM cars WHERE price IS NULL OR brand = 'Kia'").getRows().size() == 2, "OR и IS NULL");
        check(q(db, "SELECT * FROM cars WHERE NOT (brand = 'Toyota')").getRows().size() == 3, "NOT и скобки");
        check(q(db, "SELECT * FROM sales WHERE day >= '2026-04-01'").getRows().size() == 1, "сравнение дат");
        check(q(db, "SELECT * FROM cars WHERE price <> 25000").getRows().size() == 3, "<> пропускает NULL");

        q(db, "UPDATE cars SET price = 30000 WHERE id = 2");
        check(rows(db, "SELECT price FROM cars WHERE id = 2").equals("[[30000.0]]"), "UPDATE");
        check(fails(db, "UPDATE cars SET id = 1 WHERE id = 2"), "UPDATE: дубликат ключа");
        check(fails(db, "UPDATE cars SET id = 7 WHERE price > 0"), "UPDATE: один ключ многим строкам");
        check(fails(db, "UPDATE sales SET car_id = 77 WHERE id = 1"), "UPDATE: неверный внешний ключ");
        check(fails(db, "UPDATE cars SET id = 100 WHERE id = 1"), "UPDATE: нельзя менять ключ, на который ссылаются");
        q(db, "UPDATE cars SET id = 50 WHERE id = 5");
        check(rows(db, "SELECT brand FROM cars WHERE id = 50").equals("[[Kia]]"), "смена ключа без ссылок");

        check(fails(db, "DELETE FROM cars WHERE id = 1"), "DELETE: на строку есть ссылки");
        check(fails(db, "DROP TABLE cars"), "DROP: на таблицу есть ссылки");
        q(db, "DELETE FROM cars WHERE brand = 'Toyota' AND id = 4");
        check(db.table("cars").size() == 4, "DELETE по условию");

        String s1 = rows(db, "SELECT * FROM cars WHERE brand = 'Audi'");
        String s2 = rows(db, "SELECT * FROM cars WHERE price > 16000");
        String s3 = rows(db, "SELECT * FROM cars WHERE price <= 30000.0 AND brand = 'BMW'");
        check(q(db, "SELECT * FROM cars WHERE brand = 'Audi'").toString().contains("полный перебор"), "без индекса: полный перебор");
        q(db, "CREATE INDEX idx_cars_brand ON cars (brand)");
        q(db, "CREATE INDEX idx_cars_price ON cars (price)");
        check(fails(db, "CREATE INDEX idx_cars_brand ON cars (id)"), "повтор имени индекса");
        check(q(db, "SELECT * FROM cars WHERE brand = 'Audi'").toString().contains("idx_cars_brand"), "индекс по brand используется");
        check(q(db, "SELECT * FROM cars WHERE price > 16000").toString().contains("idx_cars_price"), "индекс по price для диапазона");
        check(q(db, "SELECT * FROM cars WHERE id = 3").toString().contains("первичному ключу"), "индекс первичного ключа");
        check(q(db, "SELECT * FROM cars WHERE brand = 'A' OR price > 1").toString().contains("полный перебор"), "OR индекс не использует");
        check(rows(db, "SELECT * FROM cars WHERE brand = 'Audi'").equals(s1), "результат с индексом = без индекса (=)");
        check(rows(db, "SELECT * FROM cars WHERE price > 16000").equals(s2), "результат с индексом = без индекса (>)");
        check(rows(db, "SELECT * FROM cars WHERE price <= 30000.0 AND brand = 'BMW'").equals(s3), "результат с индексом = без индекса (AND)");

        q(db, "INSERT INTO cars VALUES (60, 'Audi', 50000.0, TRUE)");
        q(db, "UPDATE cars SET brand = 'Skoda' WHERE id = 60");
        check(rows(db, "SELECT id FROM cars WHERE brand = 'Skoda'").equals("[[60]]"), "индекс обновился после UPDATE");
        check(rows(db, "SELECT id FROM cars WHERE brand = 'Audi'").equals("[[3]]"), "старое значение ушло из индекса");
        q(db, "DELETE FROM cars WHERE id = 2");
        check(rows(db, "SELECT id FROM cars WHERE brand = 'BMW'").equals("[]"), "индекс обновился после DELETE");
        check(rows(db, "SELECT id FROM cars WHERE brand = 'Skoda'").equals("[[60]]"), "после DELETE номера строк в индексе верные");

        String before = rows(db, "SELECT * FROM cars WHERE price > 0");
        Database db2 = new Database(new FileRepository(dir.toString()));
        check(rows(db2, "SELECT * FROM cars WHERE price > 0").equals(before), "данные загрузились с диска");
        check(db2.table("sales").size() == 2, "вторая таблица тоже загрузилась");
        check(q(db2, "SELECT * FROM cars WHERE brand = 'Skoda'").toString().contains("idx_cars_brand"), "индекс восстановился после перезапуска");
        check(fails(db2, "INSERT INTO cars VALUES (60, 'Dup', 1.0, TRUE)"), "ключи работают после перезапуска");

        Files.write(dir.resolve("cars.idx_cars_brand.idx"), new byte[]{1, 2, 3});
        Files.write(dir.resolve("cars.tbl.tmp"), new byte[]{9, 9});
        Database db3 = new Database(new FileRepository(dir.toString()));
        check(!Files.exists(dir.resolve("cars.tbl.tmp")), "недописанный .tmp удалён при старте");
        check(rows(db3, "SELECT id FROM cars WHERE brand = 'Skoda'").equals("[[60]]"), "битый индекс перестроился");
        check(q(db3, "SELECT * FROM cars WHERE brand = 'Skoda'").toString().contains("idx_cars_brand"), "перестроенный индекс работает");

        q(db3, "DROP INDEX idx_cars_brand");
        check(!Files.exists(dir.resolve("cars.idx_cars_brand.idx")), "файл индекса удалён");
        check(q(db3, "SELECT * FROM cars WHERE brand = 'Skoda'").toString().contains("полный перебор"), "после DROP INDEX снова перебор");
        q(db3, "DROP TABLE sales");
        q(db3, "DROP TABLE cars");
        check(db3.tables().isEmpty() && !Files.exists(dir.resolve("cars.tbl")), "DROP TABLE убрал файлы");

        int n = 0;
        Table t = null;
        for (Table x : db2.tables()) t = x;
        if (t != null) for (Object[] r : t) n++;
        check(n == t.size(), "обход таблицы через Iterator");

        System.out.println();
        System.out.println("Успешно: " + ok + ", провалено: " + bad);
        if (bad > 0) System.exit(1);
    }
}
