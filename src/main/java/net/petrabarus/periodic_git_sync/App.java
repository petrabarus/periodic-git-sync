package net.petrabarus.periodic_git_sync;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            FileWriter writer = new FileWriter(file);
            writer.write(name);
            writer.close();
            return name;
        } catch (IOException ex) {
            Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }

    public static void printCommits(Git git) throws IOException, GitAPIException {
        Iterable<RevCommit> commits1 = git.log().all().call();
        System.out.println("Current commit");
        for (RevCommit commit : commits1) {
            System.out.println("\t" + commit.getName()); //The hash
        }
    }
}
