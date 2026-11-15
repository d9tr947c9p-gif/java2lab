package dbms.store;

import dbms.db.Index;
import dbms.db.Repository;
import dbms.db.Table;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Хранит таблицы и индексы в папке: по одному файлу на таблицу и на индекс. */
public class FileRepository implements Repository {
    private final Path dir;

    public FileRepository(String d) throws IOException {
        dir = Paths.get(d);
        Files.createDirectories(dir);
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.tmp")) {
            for (Path p : ds) Files.delete(p);
        }
    }

    private Path tblFile(String tn) {
        return dir.resolve(tn.toLowerCase() + ".tbl");
    }

    private Path idxFile(String tn, String in) {
        return dir.resolve(tn.toLowerCase() + "." + in + ".idx");
    }

    private Object read(Path p) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(p)))) {
            return in.readObject();
        }
    }

    private void write(Path p, Object o) throws IOException {
        Path tmp = p.resolveSibling(p.getFileName() + ".tmp");
        try (FileOutputStream fos = new FileOutputStream(tmp.toFile());
             ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(fos))) {
            out.writeObject(o);
            out.flush();
            fos.getFD().sync();
        }
        try {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, p, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public List<Table> loadAll() throws IOException {
        List<Table> res = new ArrayList<>();
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "*.tbl")) {
            for (Path p : ds) {
                try {
                    Table t = (Table) read(p);
                    boolean rebuilt = false;
                    for (String in : t.getIdefs().keySet()) {
                        boolean good;
                        try {
                            good = t.attach((Index) read(idxFile(t.getName(), in)));
                        } catch (IOException | ClassNotFoundException | ClassCastException e) {
                            good = false;
                        }
                        if (!good) {
                            rebuilt = true;
                            System.err.println("Индекс " + in + " устарел или повреждён, строю заново");
                        }
                    }
                    t.fixIndexes();
                    if (rebuilt) save(t);
                    res.add(t);
                } catch (IOException | ClassNotFoundException e) {
                    System.err.println("Файл " + p.getFileName() + " не прочитался: " + e.getMessage());
                }
            }
        }
        res.sort(Comparator.comparing(Table::getName));
        return res;
    }

    public void save(Table t) throws IOException {
        write(tblFile(t.getName()), t);
        for (Index ix : t.userIndexes()) write(idxFile(t.getName(), ix.getName()), ix);
    }

    public void delete(Table t) throws IOException {
        Files.deleteIfExists(tblFile(t.getName()));
        for (String in : t.getIdefs().keySet()) Files.deleteIfExists(idxFile(t.getName(), in));
    }

    public void deleteIndex(Table t, String name) throws IOException {
        Files.deleteIfExists(idxFile(t.getName(), name));
    }
}
