package dbms.sql;

import dbms.db.Database;
import dbms.db.Result;

/** DROP TABLE. */
public class DropTable implements Statement {
    private final String name;

    public DropTable(String name) {
        this.name = name;
    }

    public Result run(Database db) {
        db.drop(name);
        return new Result("Таблица " + name + " удалена");
    }
}
