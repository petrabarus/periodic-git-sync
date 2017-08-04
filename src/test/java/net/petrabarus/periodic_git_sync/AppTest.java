package net.petrabarus.periodic_git_sync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import static org.junit.Assert.assertTrue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class AppTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testCreateNewRepo() throws GitAPIException {
        System.out.println(folder.getRoot());
        Git.init().setDirectory(folder.getRoot())
                    .call();
        Path gitPath = Paths.get(folder.getRoot().toString())
                .resolve(".git");
        assertTrue(Files.exists(gitPath));
    }
}
