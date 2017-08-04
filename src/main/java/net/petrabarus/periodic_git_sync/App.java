package net.petrabarus.periodic_git_sync;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.DepthWalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

public class App {

    public static void main(String[] argv) throws IOException, GitAPIException {
        Path path = Paths.get(argv[0]);
        Git git = loadOrCreateRepo(path);

        String name = createRandomFile(path);
        if (name == null) {
            return;
        }

        git.add().addFilepattern(name).call();

        git.commit().setMessage("Add file: " + name).call();

        printCommits(git);
    }

    public static Git loadOrCreateRepo(Path dirPath) throws GitAPIException {
        System.out.println(dirPath);
        File directory = dirPath.toFile();
        Git git = Git.init().setDirectory(directory).call();
        return git;
    }

    public static String createRandomFile(Path directory) {
        try {
            String name = UUID.randomUUID().toString();
            File file = directory.resolve(name).toFile();
            file.createNewFile();
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("Content: " + name);
            }
            return name;
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static void printCommits(Git git) throws IOException, GitAPIException {
        Iterable<RevCommit> commits1 = git.log().all().call();
        Iterator<RevCommit> iterator = commits1.iterator();

        RevCommit next = iterator.next();
        System.out.println(next.getName());
        while (iterator.hasNext()) {
            RevCommit current = iterator.next();
            System.out.println(current.getName());
            printDiff(git, current, next);
            next = current;
        }
    }

    public static CanonicalTreeParser getTreeParser(Git git, RevCommit commit) throws IOException {
        try (RevWalk walk = new RevWalk(git.getRepository(), 0);
                ObjectReader reader = git.getRepository().newObjectReader()) {
            return new CanonicalTreeParser(null, reader, commit.getTree());
        }
    }

    public static void printDiff(Git git, RevCommit prev, RevCommit next) throws IOException, GitAPIException {
        System.out.println("-------------------------------------------------------------------------------------------");
        CanonicalTreeParser oldTreeIterator = getTreeParser(git, prev);
        CanonicalTreeParser newTreeIterator = getTreeParser(git, next);
        try (OutputStream outputStream = new ByteArrayOutputStream();
                DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(git.getRepository());
            formatter.format(oldTreeIterator, newTreeIterator);
            System.out.println(outputStream.toString());
        }
        System.out.println("===========================================================================================");
    }
}
