package net.petrabarus.periodic_git_sync;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.DepthWalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

public class App {

    @Parameter(names = "--directory")
    public String directory;

    @Parameter(names = "--log")
    public String log;

    @Parameter(names = "--duration")
    public Integer duration = 60000;

    private Git git;

    private File logFile;

    public static class PeriodicCommitTask implements Runnable {

        private final Git git;

        private final File logFile;

        public PeriodicCommitTask(Git git, File logFile) {
            this.git = git;
            this.logFile = logFile;
        }

        @Override
        public void run() {
            System.out.println("Committing files");
            try {
                git.add().addFilepattern(".").call();
                String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                git.commit().setMessage("Commit " + timestamp).call();

                Iterable<RevCommit> commits = git.log().setMaxCount(2).all().call();
                Iterator<RevCommit> iterator = commits.iterator();
                RevCommit second = iterator.next();
                RevCommit first = iterator.next();
                String diffText = getDiffText(git, first, second);
                writeDiffText(diffText);

            } catch (GitAPIException ex) {
                //
            } catch (IncorrectObjectTypeException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RevisionSyntaxException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(App.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        private void writeDiffText(String diffText) {
            try (FileWriter fileWriter = new FileWriter(logFile, true);
                    BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {
                bufferedWriter.write(diffText);
                bufferedWriter.write("=========================================\n");
            } catch (IOException ex) {

            }
        }
    }

    public static void main(String[] argv) throws IOException, GitAPIException {
        App app = new App();
        JCommander.newBuilder()
                .addObject(app)
                .build()
                .parse(argv);
        app.run();
    }

    public void run() throws GitAPIException, IOException {
        git = loadOrCreateRepo(Paths.get(directory));
        logFile = Paths.get(log).toFile();
        createFileIfNotExists();
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        PeriodicCommitTask task = new PeriodicCommitTask(git, logFile);
        scheduler.scheduleAtFixedRate(task, 0, duration, TimeUnit.SECONDS);
    }

    private void createFileIfNotExists() throws IOException {
        if (!logFile.exists()) {
            logFile.createNewFile();
        }
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

    public static CanonicalTreeParser getTreeParser(Git git, RevCommit commit) throws IOException {
        ObjectReader reader = git.getRepository().newObjectReader();
        return new CanonicalTreeParser(null, reader, commit.getTree());
    }

    public static String getDiffText(Git git, RevCommit prev, RevCommit next) throws IOException, GitAPIException {
        CanonicalTreeParser oldTreeIterator = getTreeParser(git, prev);
        CanonicalTreeParser newTreeIterator = getTreeParser(git, next);
        try (OutputStream outputStream = new ByteArrayOutputStream();
                DiffFormatter formatter = new DiffFormatter(outputStream)) {
            formatter.setRepository(git.getRepository());
            formatter.format(oldTreeIterator, newTreeIterator);
            return outputStream.toString();
        }
    }
}
