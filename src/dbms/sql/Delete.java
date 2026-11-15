package dbms.sql;

import dbms.db.Cond;
import dbms.db.Database;
import dbms.db.Result;
import dbms.db.Table;

/** DELETE FROM ... WHERE. */
public class Delete implements Statement {
    private final String tn;
    private final Cond where;

    public Delete(String tn, Cond where) {
        this.tn = tn;
        this.where = where;
    }

    public Result run(Database db) {
        Table t = db.table(tn);
        Cond c = where == null ? null : where.bind(t);
        int n = db.delete(t, c);
        return new Result("Удалено строк: " + n);
    }
}
