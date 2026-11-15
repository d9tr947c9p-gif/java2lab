package dbms.db;

import java.util.ArrayList;
import java.util.List;

/** Результат запроса: либо таблица с данными, либо просто сообщение. */
public class Result {
    private final List<String> head;
    private final List<Object[]> rows;
    private final String msg;
    private String info;

    public Result(String msg) {
        this.msg = msg;
        this.head = null;
        this.rows = null;
    }

    public Result(List<String> head, List<Object[]> rows) {
        this.head = head;
        this.rows = rows;
        this.msg = null;
    }

    public void setInfo(String s) {
        info = s;
    }

    public boolean isTable() {
        return head != null;
    }

    public List<Object[]> getRows() {
        return rows;
    }

    private static String show(Object o) {
        return o == null ? "NULL" : String.valueOf(o);
    }

    private static String rep(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(c);
        return sb.toString();
    }

    private static String line(int[] w) {
        StringBuilder sb = new StringBuilder("+");
        for (int x : w) sb.append(rep('-', x + 2)).append('+');
        return sb.append('\n').toString();
    }

    private static String row(String[] s, int[] w) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < s.length; i++)
            sb.append(' ').append(s[i]).append(rep(' ', w[i] - s[i].length())).append(" |");
        return sb.append('\n').toString();
    }

    public String toString() {
        if (!isTable()) return msg;
        int n = head.size();
        String[] h = head.toArray(new String[0]);
        int[] w = new int[n];
        for (int i = 0; i < n; i++) w[i] = h[i].length();
        List<String[]> cells = new ArrayList<>();
        for (Object[] r : rows) {
            String[] s = new String[n];
            for (int i = 0; i < n; i++) {
                s[i] = show(r[i]);
                w[i] = Math.max(w[i], s[i].length());
            }
            cells.add(s);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(line(w)).append(row(h, w)).append(line(w));
        for (String[] s : cells) sb.append(row(s, w));
        sb.append(line(w));
        sb.append("Строк: ").append(rows.size());
        if (info != null) sb.append(" (").append(info).append(")");
        return sb.toString();
    }
}
