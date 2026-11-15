package dbms.sql;

import dbms.db.Database;
import dbms.db.Result;

/** CREATE INDEX. */
public class CreateIndex implements Statement {
    private final String name, table, col;

    public CreateIndex(String name, String table, String col) {
        this.name = name;
        this.table = table;
        this.col = col;
    }

    public Result run(Database db) {
        db.createIndex(name, table, col);
        return new Result("Индекс " + name + " создан");
    }
}
