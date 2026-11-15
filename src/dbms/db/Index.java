package dbms.db;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Индекс по одному столбцу: упорядоченное дерево значение -> номера строк. */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Index implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String col;
    private final int ci;
    private long ver;
    private final TreeMap<Comparable, List<Integer>> m = new TreeMap<>();

    public Index(String name, String col, int ci) {
        this.name = name;
        this.col = col;
        this.ci = ci;
    }

    public String getName() {
        return name;
    }

    public String getCol() {
        return col;
    }

    public int getCi() {
        return ci;
    }

    public long getVer() {
        return ver;
    }

    public void setVer(long v) {
        ver = v;
    }

    public void clear() {
        m.clear();
    }

    public void put(Object k, int pos) {
        if (k == null) return;
        List<Integer> l = m.get(k);
        if (l == null) {
            l = new ArrayList<>();
            m.put((Comparable) k, l);
        }
        l.add(pos);
    }

    public List<Integer> get(Object k) {
        List<Integer> l = m.get(k);
        if (l == null) return Collections.emptyList();
        return l;
    }

    public List<Integer> lookup(Cond.Op op, Object v) {
        Comparable k = (Comparable) v;
        switch (op) {
            case EQ:
                return get(k);
            case LT:
                return range(null, false, k, false);
            case LE:
                return range(null, false, k, true);
            case GT:
                return range(k, false, null, false);
            case GE:
                return range(k, true, null, false);
            default:
                throw new DbException("Эта операция не использует индекс");
        }
    }

    private List<Integer> range(Comparable lo, boolean loInc, Comparable hi, boolean hiInc) {
        NavigableMap<Comparable, List<Integer>> sub;
        if (lo == null) sub = m.headMap(hi, hiInc);
        else if (hi == null) sub = m.tailMap(lo, loInc);
        else sub = m.subMap(lo, loInc, hi, hiInc);
        List<Integer> res = new ArrayList<>();
        for (List<Integer> l : sub.values()) res.addAll(l);
        Collections.sort(res);
        return res;
    }
}
