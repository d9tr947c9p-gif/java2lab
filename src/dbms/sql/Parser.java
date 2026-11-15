package dbms.sql;

import dbms.db.Column;
import dbms.db.Cond;
import dbms.db.DbException;
import dbms.db.Type;

import java.util.ArrayList;
import java.util.List;

/** Разбирает текст запроса и превращает его в объект Statement. */
public class Parser {
    private static final int WORD = 0, NUM = 1, STR = 2, SYM = 3, END = 4;

    private static class Tok {
        final int kind;
        final String s;

        Tok(int kind, String s) {
            this.kind = kind;
            this.s = s;
        }
    }

    private final List<Tok> ts = new ArrayList<>();
    private int p;

    private Parser(String sql) {
        lex(sql);
    }

    public static Statement parse(String sql) {
        return new Parser(sql).statement();
    }

    public static List<String> split(String text) {
        List<String> res = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean q = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\'') q = !q;
            if (c == ';' && !q) {
                if (sb.toString().trim().length() > 0) res.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        if (sb.toString().trim().length() > 0) res.add(sb.toString().trim());
        return res;
    }

    private void lex(String s) {
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
            } else if (Character.isLetter(c) || c == '_') {
                int j = i;
                while (j < n && (Character.isLetterOrDigit(s.charAt(j)) || s.charAt(j) == '_')) j++;
                ts.add(new Tok(WORD, s.substring(i, j)));
                i = j;
            } else if (Character.isDigit(c) || (c == '-' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int j = i + 1;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                ts.add(new Tok(NUM, s.substring(i, j)));
                i = j;
            } else if (c == '\'') {
                StringBuilder sb = new StringBuilder();
                int j = i + 1;
                while (true) {
                    if (j >= n) throw new DbException("Не закрыта кавычка");
                    char d = s.charAt(j);
                    if (d == '\'') {
                        if (j + 1 < n && s.charAt(j + 1) == '\'') {
                            sb.append('\'');
                            j += 2;
                            continue;
                        }
                        break;
                    }
                    sb.append(d);
                    j++;
                }
                ts.add(new Tok(STR, sb.toString()));
                i = j + 1;
            } else {
                String two = i + 1 < n ? s.substring(i, i + 2) : "";
                if (two.equals("<=") || two.equals(">=") || two.equals("!=") || two.equals("<>")) {
                    ts.add(new Tok(SYM, two));
                    i += 2;
                } else if ("(),;*=<>".indexOf(c) >= 0) {
                    ts.add(new Tok(SYM, String.valueOf(c)));
                    i++;
                } else {
                    throw new DbException("Неожиданный символ: " + c);
                }
            }
        }
    }

    private Tok peek() {
        return p < ts.size() ? ts.get(p) : new Tok(END, "");
    }

    private Tok next() {
        Tok t = peek();
        p++;
        return t;
    }

    private String show(Tok t) {
        return t.kind == END ? "конец запроса" : "'" + t.s + "'";
    }

    private boolean isWord(String w) {
        Tok t = peek();
        return t.kind == WORD && t.s.equalsIgnoreCase(w);
    }

    private boolean eatWord(String w) {
        if (!isWord(w)) return false;
        p++;
        return true;
    }

    private void need(String w) {
        if (!eatWord(w)) throw new DbException("Ожидалось " + w + ", а найдено " + show(peek()));
    }

    private boolean eatSym(String x) {
        Tok t = peek();
        if (t.kind == SYM && t.s.equals(x)) {
            p++;
            return true;
        }
        return false;
    }

    private void needSym(String x) {
        if (!eatSym(x)) throw new DbException("Ожидалось " + x + ", а найдено " + show(peek()));
    }

    private String ident() {
        Tok t = next();
        if (t.kind != WORD) throw new DbException("Ожидалось имя, а найдено " + show(t));
        return t.s;
    }

    private Statement statement() {
        Statement st;
        if (eatWord("SELECT")) st = select();
        else if (eatWord("INSERT")) st = insert();
        else if (eatWord("UPDATE")) st = update();
        else if (eatWord("DELETE")) st = delete();
        else if (eatWord("CREATE")) st = create();
        else if (eatWord("DROP")) st = drop();
        else throw new DbException("Непонятная команда: " + show(peek()));
        eatSym(";");
        if (peek().kind != END) throw new DbException("Лишнее в конце запроса: " + show(peek()));
        return st;
    }

    private Statement create() {
        if (eatWord("TABLE")) {
            String n = ident();
            needSym("(");
            List<Column> cs = new ArrayList<>();
            do {
                String cn = ident();
                Type ty = Type.of(ident());
                boolean pk = false;
                String ref = null, rc = null;
                while (true) {
                    if (eatWord("PRIMARY")) {
                        need("KEY");
                        pk = true;
                    } else if (eatWord("REFERENCES")) {
                        ref = ident();
                        if (eatSym("(")) {
                            rc = ident();
                            needSym(")");
                        }
                    } else {
                        break;
                    }
                }
                cs.add(new Column(cn, ty, pk, ref, rc));
            } while (eatSym(","));
            needSym(")");
            return new CreateTable(n, cs);
        }
        if (eatWord("INDEX")) {
            String in = ident();
            need("ON");
            String tn = ident();
            needSym("(");
            String c = ident();
            needSym(")");
            return new CreateIndex(in, tn, c);
        }
        throw new DbException("После CREATE ожидалось TABLE или INDEX");
    }

    private Statement drop() {
        if (eatWord("TABLE")) return new DropTable(ident());
        if (eatWord("INDEX")) return new DropIndex(ident());
        throw new DbException("После DROP ожидалось TABLE или INDEX");
    }

    private Statement insert() {
        need("INTO");
        String tn = ident();
        List<String> cs = null;
        if (eatSym("(")) {
            cs = new ArrayList<>();
            do cs.add(ident()); while (eatSym(","));
            needSym(")");
        }
        need("VALUES");
        List<List<Object>> vs = new ArrayList<>();
        do {
            needSym("(");
            List<Object> row = new ArrayList<>();
            do row.add(value()); while (eatSym(","));
            needSym(")");
            vs.add(row);
        } while (eatSym(","));
        return new Insert(tn, cs, vs);
    }

    private Statement select() {
        List<String> cs = null;
        if (!eatSym("*")) {
            cs = new ArrayList<>();
            do cs.add(ident()); while (eatSym(","));
        }
        need("FROM");
        String tn = ident();
        Cond w = eatWord("WHERE") ? cond() : null;
        return new Select(tn, cs, w);
    }

    private Statement update() {
        String tn = ident();
        need("SET");
        List<String> cs = new ArrayList<>();
        List<Object> vs = new ArrayList<>();
        do {
            cs.add(ident());
            needSym("=");
            vs.add(value());
        } while (eatSym(","));
        Cond w = eatWord("WHERE") ? cond() : null;
        return new Update(tn, cs, vs, w);
    }

    private Statement delete() {
        need("FROM");
        String tn = ident();
        Cond w = eatWord("WHERE") ? cond() : null;
        return new Delete(tn, w);
    }

    private Object value() {
        Tok t = next();
        if (t.kind == NUM) {
            try {
                if (t.s.indexOf('.') >= 0) return Double.parseDouble(t.s);
                return Long.parseLong(t.s);
            } catch (NumberFormatException e) {
                throw new DbException("Плохое число: " + t.s);
            }
        }
        if (t.kind == STR) return t.s;
        if (t.kind == WORD) {
            if (t.s.equalsIgnoreCase("TRUE")) return true;
            if (t.s.equalsIgnoreCase("FALSE")) return false;
            if (t.s.equalsIgnoreCase("NULL")) return null;
        }
        throw new DbException("Ожидалось значение, а найдено " + show(t));
    }

    private Cond cond() {
        Cond c = and();
        while (eatWord("OR")) c = new Cond.Or(c, and());
        return c;
    }

    private Cond and() {
        Cond c = not();
        while (eatWord("AND")) c = new Cond.And(c, not());
        return c;
    }

    private Cond not() {
        if (eatWord("NOT")) return new Cond.Not(not());
        if (eatSym("(")) {
            Cond c = cond();
            needSym(")");
            return c;
        }
        String col = ident();
        if (eatWord("IS")) {
            boolean n = eatWord("NOT");
            need("NULL");
            return new Cond.Nul(col, n);
        }
        Tok t = next();
        if (t.kind != SYM) throw new DbException("Ожидался знак сравнения, а найдено " + show(t));
        return new Cond.Cmp(col, Cond.Op.of(t.s), value());
    }
}
