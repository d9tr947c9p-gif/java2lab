package dbms.db;

/** Ошибка при работе с базой данных. */
public class DbException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public DbException(String m) {
        super(m);
    }
}
