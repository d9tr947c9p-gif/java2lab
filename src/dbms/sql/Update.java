package dbms.sql;

import dbms.db.Column;
import dbms.db.Cond;
import dbms.db.Database;
import dbms.db.DbException;
import dbms.db.Result;
import dbms.db.Table;

import java.util.List;

/** UPDATE ... SET ... WHERE. */
public class Update implements Statement {
    private final String tn;
    private final List<String> cols;
    private final List<Object> vals;
    private final Cond where;

    public Update(String tn, List<String> cols, List<Object> vals, Cond where) {
        this.tn = tn;
        this.cols = cols;
        this.vals = vals;
        this.where = where;
    }

    public Result run(Database db) {
        Table t = db.table(tn);
        Cond c = where == null ? null : where.bind(t);
        int[] ci = new int[cols.size()];
        Object[] vv = new Object[ci.length];
        for (int i = 0; i < ci.length; i++) {
            ci[i] = t.col(cols.get(i));
            Column col = t.getCols().get(ci[i]);
            vv[i] = col.getType().conv(vals.get(i));
            if (col.isPk() && vv[i] == null) throw new DbException("Первичный ключ не может быть NULL");
        }
        int n = db.update(t, c, ci, vv);
        return new Result("Обновлено строк: " + n);
    }
}
