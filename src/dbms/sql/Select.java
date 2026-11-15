package dbms.sql;

import dbms.db.Cond;
import dbms.db.Database;
import dbms.db.Result;
import dbms.db.Table;

import java.util.ArrayList;
import java.util.List;

/** SELECT. */
public class Select implements Statement {
    private final String tn;
    private final List<String> cols;
    private final Cond where;

    public Select(String tn, List<String> cols, Cond where) {
        this.tn = tn;
        this.cols = cols;
        this.where = where;
    }

    private Object[] pick(Object[] r, int[] ci) {
        Object[] o = new Object[ci.length];
        for (int i = 0; i < ci.length; i++) o[i] = r[ci[i]];
        return o;
    }

    public Result run(Database db) {
        Table t = db.table(tn);
        Cond c = where == null ? null : where.bind(t);
        int[] ci;
        if (cols == null) {
            ci = new int[t.getCols().size()];
            for (int i = 0; i < ci.length; i++) ci[i] = i;
        } else {
            ci = new int[cols.size()];
            for (int i = 0; i < ci.length; i++) ci[i] = t.col(cols.get(i));
        }
        List<String> head = new ArrayList<>();
        for (int i : ci) head.add(t.getCols().get(i).getName());
        List<Object[]> out = new ArrayList<>();
        if (c == null) {
            for (Object[] r : t) out.add(pick(r, ci));
        } else {
            for (int p : t.match(c)) out.add(pick(t.get(p), ci));
        }
        Result res = new Result(head, out);
        res.setInfo(t.plan(c));
        return res;
    }
}
