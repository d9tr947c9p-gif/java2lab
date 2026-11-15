package dbms.db;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Таблица в памяти: столбцы, строки и индексы. Можно обходить через for-each. */
public class Table implements Serializable, Iterable<Object[]> {
    private static final long serialVersionUID = 1L;
    public static final String PK = "#pk";

    private final String name;
    private final List<Column> cols;
    private final List<Object[]> rows = new ArrayList<>();
    private final Map<String, String> idefs = new LinkedHashMap<>();
    private long ver;
    private transient Map<String, Index> idx = new LinkedHashMap<>();

    public Table(String name, List<Column> cols) {
        if (cols.isEmpty()) throw new DbException("В таблице нет столбцов");
        Set<String> seen = new HashSet<>();
        int pks = 0;
        for (Column c : cols) {
            if (!seen.add(c.getName().toLowerCase()))
                throw new DbException("Столбец " + c.getName() + " указан дважды");
            if (c.isPk()) pks++;
        }
        if (pks != 1) throw new DbException("Нужен ровно один PRIMARY KEY");
        this.name = name;
        this.cols = new ArrayList<>(cols);
        fixIndexes();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        idx = new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public List<Column> getCols() {
        return Collections.unmodifiableList(cols);
    }

    public int size() {
        return rows.size();
    }

    public Object[] get(int i) {
        return rows.get(i);
    }

    public Iterator<Object[]> iterator() {
        return Collections.unmodifiableList(rows).iterator();
    }

    public int col(String n) {
        for (int i = 0; i < cols.size(); i++)
            if (cols.get(i).getName().equalsIgnoreCase(n)) return i;
        throw new DbException("Нет столбца " + n + " в таблице " + name);
    }

    public int pkCol() {
        for (int i = 0; i < cols.size(); i++)
            if (cols.get(i).isPk()) return i;
        return -1;
    }

    public Map<String, String> getIdefs() {
        return Collections.unmodifiableMap(idefs);
    }

    public boolean hasIndex(String n) {
        return idefs.containsKey(n.toLowerCase());
    }

    public List<Index> userIndexes() {
        List<Index> l = new ArrayList<>();
        for (Map.Entry<String, Index> e : idx.entrySet())
            if (!e.getKey().equals(PK)) l.add(e.getValue());
        return l;
    }

    public Index findIndex(int ci) {
        for (Index ix : idx.values())
            if (ix.getCi() == ci) return ix;
        return null;
    }

    public boolean attach(Index ix) {
        String c = idefs.get(ix.getName());
        if (c == null || !c.equalsIgnoreCase(ix.getCol()) || ix.getVer() != ver) return false;
        idx.put(ix.getName(), ix);
        return true;
    }

    public void fixIndexes() {
        if (!idx.containsKey(PK)) makeIndex(PK, cols.get(pkCol()).getName());
        for (Map.Entry<String, String> e : idefs.entrySet())
            if (!idx.containsKey(e.getKey())) makeIndex(e.getKey(), e.getValue());
    }

    private void makeIndex(String n, String c) {
        int ci = col(c);
        Index ix = new Index(n, cols.get(ci).getName(), ci);
        for (int i = 0; i < rows.size(); i++) ix.put(rows.get(i)[ci], i);
        ix.setVer(ver);
        idx.put(n, ix);
    }

    public void addIndex(String n, String c) {
        n = n.toLowerCase();
        if (idefs.containsKey(n)) throw new DbException("Индекс " + n + " уже есть");
        int ci = col(c);
        idefs.put(n, cols.get(ci).getName());
        makeIndex(n, c);
    }

    public void removeIndex(String n) {
        n = n.toLowerCase();
        idefs.remove(n);
        idx.remove(n);
    }

    private void reindex() {
        for (Index ix : idx.values()) {
            ix.clear();
            for (int i = 0; i < rows.size(); i++) ix.put(rows.get(i)[ix.getCi()], i);
        }
    }

    private void touch() {
        ver++;
        for (Index ix : idx.values()) ix.setVer(ver);
    }

    public void insert(List<Object[]> nr) {
        int pk = pkCol();
        Index pi = idx.get(PK);
        Set<Object> seen = new HashSet<>();
        for (Object[] r : nr) {
            Object k = r[pk];
            if (k == null) throw new DbException("Первичный ключ не может быть NULL");
            if (!pi.get(k).isEmpty() || !seen.add(k))
                throw new DbException("Повтор первичного ключа: " + k);
        }
        for (Object[] r : nr) {
            rows.add(r);
            int p = rows.size() - 1;
            for (Index ix : idx.values()) ix.put(r[ix.getCi()], p);
        }
        if (!nr.isEmpty()) touch();
    }

    public int update(List<Integer> pos, int[] ci, Object[] vv) {
        int pk = pkCol();
        for (int k = 0; k < ci.length; k++) {
            if (ci[k] != pk) continue;
            if (vv[k] == null) throw new DbException("Первичный ключ не может быть NULL");
            if (pos.size() > 1) throw new DbException("Повтор первичного ключа: " + vv[k]);
            if (pos.size() == 1) {
                Object old = rows.get(pos.get(0))[pk];
                if (!vv[k].equals(old) && !idx.get(PK).get(vv[k]).isEmpty())
                    throw new DbException("Повтор первичного ключа: " + vv[k]);
            }
        }
        for (int p : pos) {
            Object[] r = rows.get(p);
            for (int k = 0; k < ci.length; k++) r[ci[k]] = vv[k];
        }
        if (!pos.isEmpty()) {
            touch();
            boolean re = false;
            for (int c : ci)
                if (findIndex(c) != null) re = true;
            if (re) reindex();
        }
        return pos.size();
    }

    public int delete(List<Integer> pos) {
        if (pos.isEmpty()) return 0;
        Set<Integer> del = new HashSet<>(pos);
        List<Object[]> keep = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++)
            if (!del.contains(i)) keep.add(rows.get(i));
        rows.clear();
        rows.addAll(keep);
        touch();
        reindex();
        return pos.size();
    }

    public List<Integer> find(int ci, Object key) {
        Index ix = findIndex(ci);
        if (ix != null) return ix.get(key);
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++)
            if (key.equals(rows.get(i)[ci])) res.add(i);
        return res;
    }

    private Cond.Cmp pick(Cond c) {
        if (c == null) return null;
        Cond.Cmp best = null;
        for (Cond x : c.parts()) {
            if (!(x instanceof Cond.Cmp)) continue;
            Cond.Cmp m = (Cond.Cmp) x;
            if (m.getOp() == Cond.Op.NE || findIndex(m.getCi()) == null) continue;
            if (m.getOp() == Cond.Op.EQ) return m;
            if (best == null) best = m;
        }
        return best;
    }

    public List<Integer> match(Cond c) {
        List<Integer> res = new ArrayList<>();
        Cond.Cmp m = pick(c);
        if (m != null) {
            Index ix = findIndex(m.getCi());
            for (int p : ix.lookup(m.getOp(), m.getVal()))
                if (c.test(rows.get(p))) res.add(p);
            return res;
        }
        for (int i = 0; i < rows.size(); i++)
            if (c == null || c.test(rows.get(i))) res.add(i);
        return res;
    }

    public String plan(Cond c) {
        Cond.Cmp m = pick(c);
        if (m == null) return "полный перебор";
        Index ix = findIndex(m.getCi());
        if (ix.getName().equals(PK)) return "индекс по первичному ключу";
        return "индекс " + ix.getName();
    }
}
