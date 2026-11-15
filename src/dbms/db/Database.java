package dbms.db;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** База данных: набор таблиц. Следит за связями между таблицами и сохраняет изменения. */
public class Database {
    private final Map<String, Table> tabs = new LinkedHashMap<>();
    private final Repository repo;

    public Database(Repository repo) throws IOException {
        this.repo = repo;
        for (Table t : repo.loadAll()) tabs.put(t.getName().toLowerCase(), t);
    }

    public Table table(String n) {
        Table t = tabs.get(n.toLowerCase());
        if (t == null) throw new DbException("Нет таблицы " + n);
        return t;
    }

    public Collection<Table> tables() {
        return Collections.unmodifiableCollection(tabs.values());
    }

    private void save(Table t) {
        try {
            repo.save(t);
        } catch (IOException e) {
            throw new DbException("Не удалось сохранить " + t.getName() + ": " + e.getMessage());
        }
    }

    public void create(Table t) {
        String k = t.getName().toLowerCase();
        if (tabs.containsKey(k)) throw new DbException("Таблица " + t.getName() + " уже есть");
        for (Column c : t.getCols()) {
            if (c.getRef() == null) continue;
            Table rt = table(c.getRef());
            Column rk = rt.getCols().get(rt.pkCol());
            if (c.getRefCol() != null && !c.getRefCol().equalsIgnoreCase(rk.getName()))
                throw new DbException("Ссылаться можно только на ключ " + rt.getName() + "(" + rk.getName() + ")");
            if (rk.getType() != c.getType())
                throw new DbException("Тип столбца " + c.getName() + " не совпадает с ключом " + rt.getName());
        }
        save(t);
        tabs.put(k, t);
    }

    public void drop(String n) {
        Table t = table(n);
        for (Table o : tabs.values()) {
            if (o == t) continue;
            for (Column c : o.getCols())
                if (c.getRef() != null && c.getRef().equalsIgnoreCase(t.getName()))
                    throw new DbException("На таблицу " + t.getName() + " ссылается " + o.getName());
        }
        try {
            repo.delete(t);
        } catch (IOException e) {
            throw new DbException("Не удалось удалить файлы: " + e.getMessage());
        }
        tabs.remove(n.toLowerCase());
    }

    public void createIndex(String in, String tn, String col) {
        for (Table o : tabs.values())
            if (o.hasIndex(in)) throw new DbException("Индекс " + in + " уже есть");
        Table t = table(tn);
        t.addIndex(in, col);
        save(t);
    }

    public void dropIndex(String in) {
        for (Table t : tabs.values()) {
            if (!t.hasIndex(in)) continue;
            t.removeIndex(in);
            save(t);
            try {
                repo.deleteIndex(t, in.toLowerCase());
            } catch (IOException e) {
                throw new DbException("Не удалось удалить файл индекса: " + e.getMessage());
            }
            return;
        }
        throw new DbException("Нет индекса " + in);
    }

    private void checkRef(Column c, Object v) {
        Table rt = table(c.getRef());
        if (rt.find(rt.pkCol(), v).isEmpty())
            throw new DbException("В таблице " + rt.getName() + " нет записи с ключом " + v);
    }

    private boolean used(Table t, Object key) {
        for (Table o : tabs.values())
            for (int i = 0; i < o.getCols().size(); i++) {
                String r = o.getCols().get(i).getRef();
                if (r != null && r.equalsIgnoreCase(t.getName()) && !o.find(i, key).isEmpty())
                    return true;
            }
        return false;
    }

    public int insert(Table t, List<Object[]> rows) {
        for (Object[] r : rows)
            for (int i = 0; i < r.length; i++) {
                Column c = t.getCols().get(i);
                if (c.getRef() != null && r[i] != null) checkRef(c, r[i]);
            }
        t.insert(rows);
        if (!rows.isEmpty()) save(t);
        return rows.size();
    }

    public int update(Table t, Cond c, int[] ci, Object[] vv) {
        List<Integer> pos = t.match(c);
        int pk = t.pkCol();
        for (int k = 0; k < ci.length; k++) {
            Column col = t.getCols().get(ci[k]);
            if (col.getRef() != null && vv[k] != null) checkRef(col, vv[k]);
            if (ci[k] == pk)
                for (int p : pos)
                    if (used(t, t.get(p)[pk]))
                        throw new DbException("Нельзя менять ключ " + t.get(p)[pk] + ", на него есть ссылки");
        }
        int n = t.update(pos, ci, vv);
        if (n > 0) save(t);
        return n;
    }

    public int delete(Table t, Cond c) {
        List<Integer> pos = t.match(c);
        int pk = t.pkCol();
        for (int p : pos)
            if (used(t, t.get(p)[pk]))
                throw new DbException("Нельзя удалить строку с ключом " + t.get(p)[pk] + ", на неё есть ссылки");
        int n = t.delete(pos);
        if (n > 0) save(t);
        return n;
    }
}
