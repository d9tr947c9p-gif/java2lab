package dbms;

import dbms.db.Column;
import dbms.db.Database;
import dbms.db.DbException;
import dbms.db.Table;
import dbms.sql.Parser;
import dbms.store.FileRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Scanner;

/** Консольный интерфейс мини-СУБД. */
public class Main {

    public static void main(String[] args) {
        String dir = args.length > 0 ? args[0] : "data";
        Database db;
        try {
            db = new Database(new FileRepository(dir));
        } catch (IOException e) {
            System.out.println("Не удалось открыть папку с данными: " + e.getMessage());
            return;
        }
        System.out.println("Мини-СУБД, данные в папке '" + dir + "'. Команда help покажет справку.");
        Scanner sc = new Scanner(System.in);
        StringBuilder buf = new StringBuilder();
        while (true) {
            System.out.print(buf.length() == 0 ? "db> " : "... ");
            if (!sc.hasNextLine()) break;
            String line = sc.nextLine();
            String t = line.trim();
            if (buf.length() == 0) {
                if (t.isEmpty()) continue;
                if (t.equalsIgnoreCase("exit") || t.equalsIgnoreCase("quit")) break;
                if (command(db, t)) continue;
            }
            buf.append(line).append('\n');
            if (t.endsWith(";")) {
                exec(db, buf.toString(), false);
                buf.setLength(0);
            }
        }
        System.out.println("Пока!");
    }

    private static boolean command(Database db, String line) {
        String[] w = line.split("\\s+", 2);
        String c = w[0].toLowerCase();
        try {
            if (c.equals("help")) {
                help();
                return true;
            }
            if (c.equals("tables")) {
                tables(db);
                return true;
            }
            if (c.equals("desc")) {
                if (w.length < 2) System.out.println("Использование: desc имя_таблицы");
                else desc(db.table(w[1].replace(";", "").trim()));
                return true;
            }
            if (c.equals("run")) {
                if (w.length < 2) System.out.println("Использование: run файл.sql");
                else runFile(db, w[1].trim());
                return true;
            }
        } catch (DbException | IOException e) {
            System.out.println("Ошибка: " + e.getMessage());
            return true;
        }
        return false;
    }

    private static void exec(Database db, String text, boolean echo) {
        for (String q : Parser.split(text)) {
            if (echo) System.out.println("> " + q + ";");
            try {
                System.out.println(Parser.parse(q).run(db));
            } catch (DbException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Что-то пошло не так: " + e);
            }
        }
    }

    private static void runFile(Database db, String name) throws IOException {
        StringBuilder sb = new StringBuilder();
        for (String l : Files.readAllLines(Paths.get(name), StandardCharsets.UTF_8))
            if (!l.trim().startsWith("--")) sb.append(l).append('\n');
        exec(db, sb.toString(), true);
    }

    private static void tables(Database db) {
        if (db.tables().isEmpty()) System.out.println("Таблиц пока нет");
        for (Table t : db.tables()) System.out.println(t.getName() + " (строк: " + t.size() + ")");
    }

    private static void desc(Table t) {
        System.out.println("Таблица " + t.getName() + ", строк: " + t.size());
        for (Column c : t.getCols()) {
            String s = "  " + c.getName() + " " + c.getType();
            if (c.isPk()) s += " PRIMARY KEY";
            if (c.getRef() != null) s += " -> " + c.getRef();
            System.out.println(s);
        }
        for (Map.Entry<String, String> e : t.getIdefs().entrySet())
            System.out.println("  индекс " + e.getKey() + " по " + e.getValue());
    }

    private static void help() {
        System.out.println("Запросы заканчиваются на ';', можно писать в несколько строк.");
        System.out.println("  CREATE TABLE имя (столбец ТИП [PRIMARY KEY] [REFERENCES таблица], ...);");
        System.out.println("  DROP TABLE имя;");
        System.out.println("  CREATE INDEX имя ON таблица (столбец);");
        System.out.println("  DROP INDEX имя;");
        System.out.println("  INSERT INTO таблица [(столбцы)] VALUES (...), (...);");
        System.out.println("  SELECT * | столбцы FROM таблица [WHERE условие];");
        System.out.println("  UPDATE таблица SET столбец = значение, ... [WHERE условие];");
        System.out.println("  DELETE FROM таблица [WHERE условие];");
        System.out.println("Типы: INT, DOUBLE, STRING, DATE ('2024-05-01'), BOOL");
        System.out.println("Команды: tables, desc имя, run файл.sql, help, exit");
    }
}
