package dbms.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Условие из WHERE. Из него строится дерево: сравнения, AND, OR, NOT. */
public abstract class Cond {

    public enum Op {
        EQ, NE, LT, LE, GT, GE;

        public static Op of(String s) {
            switch (s) {
                case "=":
                    return EQ;
                case "!=":
                case "<>":
                    return NE;
                case "<":
                    return LT;
                case "<=":
                    return LE;
                case ">":
                    return GT;
                case ">=":
                    return GE;
                default:
                    throw new DbException("Неизвестная операция " + s);
            }
        }
    }

    public abstract boolean test(Object[] r);

    public abstract Cond bind(Table t);

    public List<Cond> parts() {
        return Collections.singletonList(this);
    }

    /** Сравнение столбца с константой. */
    public static class Cmp extends Cond {
        private final String col;
        private final Op op;
        private Object val;
        private int ci = -1;

        public Cmp(String col, Op op, Object val) {
            this.col = col;
            this.op = op;
            this.val = val;
        }

        public Op getOp() {
            return op;
        }

        public Object getVal() {
            return val;
        }

        public int getCi() {
            return ci;
        }

        public Cond bind(Table t) {
            ci = t.col(col);
            Type ty = t.getCols().get(ci).getType();
            if (val == null) throw new DbException("С NULL нужно сравнивать через IS NULL");
            if (ty == Type.BOOL && op != Op.EQ && op != Op.NE)
                throw new DbException("Для логического столбца подходят только = и !=");
            val = ty.conv(val);
            return this;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        public boolean test(Object[] r) {
            Object v = r[ci];
            if (v == null) return false;
            int d = ((Comparable) v).compareTo(val);
            switch (op) {
                case EQ:
                    return d == 0;
                case NE:
                    return d != 0;
                case LT:
                    return d < 0;
                case LE:
                    return d <= 0;
                case GT:
                    return d > 0;
                default:
                    return d >= 0;
            }
        }
    }

    /** Проверка IS NULL / IS NOT NULL. */
    public static class Nul extends Cond {
        private final String col;
        private final boolean not;
        private int ci = -1;

        public Nul(String col, boolean not) {
            this.col = col;
            this.not = not;
        }

        public Cond bind(Table t) {
            ci = t.col(col);
            return this;
        }

        public boolean test(Object[] r) {
            return (r[ci] == null) != not;
        }
    }

    public static class And extends Cond {
        private final Cond a, b;

        public And(Cond a, Cond b) {
            this.a = a;
            this.b = b;
        }

        public Cond bind(Table t) {
            a.bind(t);
            b.bind(t);
            return this;
        }

        public boolean test(Object[] r) {
            return a.test(r) && b.test(r);
        }

        public List<Cond> parts() {
            List<Cond> l = new ArrayList<>(a.parts());
            l.addAll(b.parts());
            return l;
        }
    }

    public static class Or extends Cond {
        private final Cond a, b;

        public Or(Cond a, Cond b) {
            this.a = a;
            this.b = b;
        }

        public Cond bind(Table t) {
            a.bind(t);
            b.bind(t);
            return this;
        }

        public boolean test(Object[] r) {
            return a.test(r) || b.test(r);
        }
    }

    public static class Not extends Cond {
        private final Cond c;

        public Not(Cond c) {
            this.c = c;
        }

        public Cond bind(Table t) {
            c.bind(t);
            return this;
        }

        public boolean test(Object[] r) {
            return !c.test(r);
        }
    }
}
