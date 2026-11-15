package dbms.sql;

import dbms.db.Database;
import dbms.db.Result;

/** DROP INDEX. */
public class DropIndex implements Statement {
    private final String name;

    public DropIndex(String name) {
        this.name = name;
    }

    public Result run(Database db) {
        db.dropIndex(name);
        return new Result("Индекс " + name + " удалён");
    }
}
