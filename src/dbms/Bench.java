package dbms;

import dbms.db.Database;
import dbms.db.Table;
import dbms.sql.Parser;
import dbms.store.FileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** Замеры скорости SELECT без индекса и с индексом. */
public class Bench {
    private static final String[] QS = {
            "SELECT * FROM cars WHERE brand = 'brand500'",
            "SELECT * FROM cars WHERE price > 99900",
            "SELECT * FROM cars WHERE brand = 'brand500' AND price > 50000"
    };

    private static double time(Database db, String sql, int reps) {
        for (int i = 0; i < 3; i++) Parser.parse(sql).run(db);
        double[] r = new double[5];
        for (int k = 0; k < r.length; k++) {
            long t = System.nanoTime();
            for (int i = 0; i < reps; i++) Parser.parse(sql).run(db);
            r[k] = (System.nanoTime() - t) / 1e6 / reps;
        }
        Arrays.sort(r);
        return r[r.length / 2];
    }

    private static String[] answers(Database db) {
        String[] a = new String[QS.length];
        for (int i = 0; i < QS.length; i++)
            a[i] = Arrays.deepToString(Parser.parse(QS[i]).run(db).getRows().toArray());
        return a;
    }

    public static void main(String[] args) throws IOException {
        int[] sizes = {10000, 100000, 500000};
        Path tmp = Files.createTempDirectory("dbbench");
        System.out.println("| Строк | Запрос | Найдено | Без индекса, мс | С индексом, мс | Ускорение |");
        System.out.println("|---|---|---|---|---|---|");
        List<String> extra = new ArrayList<>();
        for (int n : sizes) {
            String dir = tmp.resolve("n" + n).toString();
            Database db = new Database(new FileRepository(dir));
            Parser.parse("CREATE TABLE cars (id INT PRIMARY KEY, brand STRING, price DOUBLE, in_stock BOOL)").run(db);
            Table t = db.table("cars");
            Random rnd = new Random(42);
            List<Object[]> rows = new ArrayList<>();
            for (int i = 1; i <= n; i++)
                rows.add(new Object[]{i, "brand" + rnd.nextInt(1000), Math.round(rnd.nextDouble() * 1000000) / 10.0, rnd.nextBoolean()});
            long t0 = System.nanoTime();
            t.insert(rows);
            double insMs = (System.nanoTime() - t0) / 1e6;

            int reps = Math.max(3, 400000 / n);
            double[] a = new double[QS.length];
            int[] found = new int[QS.length];
            for (int i = 0; i < QS.length; i++) {
                a[i] = time(db, QS[i], reps);
                found[i] = Parser.parse(QS[i]).run(db).getRows().size();
            }
            String[] before = answers(db);

            t0 = System.nanoTime();
            Parser.parse("CREATE INDEX idx_cars_brand ON cars (brand)").run(db);
            Parser.parse("CREATE INDEX idx_cars_price ON cars (price)").run(db);
            double idxMs = (System.nanoTime() - t0) / 1e6;

            double[] b = new double[QS.length];
            for (int i = 0; i < QS.length; i++) b[i] = time(db, QS[i], reps * 5);
            boolean same = Arrays.equals(before, answers(db));

            for (int i = 0; i < QS.length; i++)
                System.out.printf("| %d | %d | %d | %.3f | %.3f | x%.0f |%n", n, i + 1, found[i], a[i], b[i], a[i] / b[i]);

            t0 = System.nanoTime();
            Database again = new Database(new FileRepository(dir));
            double loadMs = (System.nanoTime() - t0) / 1e6;
            extra.add(String.format("n=%d: вставка пачкой %.0f мс, CREATE INDEX x2 с записью на диск %.0f мс, загрузка базы с диска %.0f мс, строк после загрузки %d, ответы совпали: %b",
                    n, insMs, idxMs, loadMs, again.table("cars").size(), same));
        }
        System.out.println();
        for (String s : extra) System.out.println(s);
    }
}
