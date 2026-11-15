package dbms.db;

import java.io.Serializable;

/** Описание одного столбца таблицы. */
public class Column implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final Type type;
    private final boolean pk;
    private final String ref;
    private final String refCol;

    public Column(String name, Type type, boolean pk, String ref, String refCol) {
        this.name = name;
        this.type = type;
        this.pk = pk;
        this.ref = ref;
        this.refCol = refCol;
    }

    public Column(String name, Type type) {
        this(name, type, false, null, null);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type;
    }

    public boolean isPk() {
        return pk;
    }

    public String getRef() {
        return ref;
    }

    public String getRefCol() {
        return refCol;
    }
}
