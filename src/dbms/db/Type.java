package dbms.db;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Типы данных, которые можно хранить в таблице. */
public enum Type {
    INT, DOUBLE, STRING, DATE, BOOL;

    public static Type of(String s) {
        switch (s.toUpperCase()) {
            case "INT":
            case "INTEGER":
                return INT;
            case "DOUBLE":
            case "FLOAT":
                return DOUBLE;
            case "STRING":
            case "VARCHAR":
            case "TEXT":
                return STRING;
            case "DATE":
                return DATE;
            case "BOOL":
            case "BOOLEAN":
                return BOOL;
            default:
                throw new DbException("Неизвестный тип: " + s);
        }
    }

    public Object conv(Object v) {
        if (v == null) return null;
        switch (this) {
            case INT:
                if (v instanceof Integer) return v;
                if (v instanceof Long) {
                    long l = (Long) v;
                    if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) throw bad(v);
                    return (int) l;
                }
                break;
            case DOUBLE:
                if (v instanceof Double) return v;
                if (v instanceof Long) return ((Long) v).doubleValue();
                if (v instanceof Integer) return ((Integer) v).doubleValue();
                break;
            case STRING:
                if (v instanceof String) return v;
                break;
            case DATE:
                if (v instanceof LocalDate) return v;
                if (v instanceof String) {
                    try {
                        return LocalDate.parse((String) v);
                    } catch (DateTimeParseException e) {
                        throw new DbException("Неверная дата " + v + ", нужен формат гггг-мм-дд");
                    }
                }
                break;
            case BOOL:
                if (v instanceof Boolean) return v;
                break;
        }
        throw bad(v);
    }

    private DbException bad(Object v) {
        return new DbException("Значение " + v + " не подходит к типу " + this);
    }
}
