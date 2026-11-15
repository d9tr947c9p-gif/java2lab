package dbms.sql;

import dbms.db.Database;
import dbms.db.Result;

/** Один разобранный запрос. Умеет выполнить себя на базе. */
public interface Statement {
    Result run(Database db);
}
