package dbms.sql;

import dbms.db.Column;
import dbms.db.Database;
import dbms.db.Result;
import dbms.db.Table;

import java.util.List;

/** CREATE TABLE. */
public class CreateTable implements Statement {
    private final String name;
    private final List<Column> cols;

    public CreateTable(String name, List<Column> cols) {
        this.name = name;
        this.cols = cols;
    }

    public Result run(Database db) {
        db.create(new Table(name, cols));
        return new Result("Таблица " + name + " создана");
    }
}
