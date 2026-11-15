package dbms.db;

import java.io.IOException;
import java.util.List;

/** Доступ к хранилищу таблиц. Сама база не знает, где и как лежат данные. */
public interface Repository {
    List<Table> loadAll() throws IOException;

    void save(Table t) throws IOException;

    void delete(Table t) throws IOException;

    void deleteIndex(Table t, String name) throws IOException;
}
