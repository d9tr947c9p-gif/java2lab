package dbms.sql;

import dbms.db.Column;
import dbms.db.Database;
import dbms.db.DbException;
import dbms.db.Result;
import dbms.db.Table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** INSERT INTO. Можно вставить сразу несколько строк. */
public class Insert implements Statement {
    private final String tn;
    private final List<String> cols;
    private final List<List<Object>> vals;

    public Insert(String tn, List<String> cols, List<List<Object>> vals) {
        this.tn = tn;
        this.cols = cols;
        this.vals = vals;
    }

    public Result run(Database db) {
        Table t = db.table(tn);
        int n = t.getCols().size();
        int[] pos;
        if (cols == null) {
            pos = new int[n];
            for (int i = 0; i < n; i++) pos[i] = i;
        } else {
            pos = new int[cols.size()];
            Set<Integer> seen = new HashSet<>();
            for (int i = 0; i < pos.length; i++) {
                pos[i] = t.col(cols.get(i));
                if (!seen.add(pos[i])) throw new DbException("Столбец " + cols.get(i) + " указан дважды");
            }
        }
        List<Object[]> rows = new ArrayList<>();
        for (List<Object> v : vals) {
            if (v.size() != pos.length)
                throw new DbException("Значений " + v.size() + ", а столбцов " + pos.length);
            Object[] r = new Object[n];
            for (int i = 0; i < pos.length; i++) {
                Column c = t.getCols().get(pos[i]);
                r[pos[i]] = c.getType().conv(v.get(i));
            }
            rows.add(r);
        }
        int k = db.insert(t, rows);
        return new Result("Добавлено строк: " + k);
    }
}
