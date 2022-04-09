package it.isislab.p2p.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.isislab.p2p.git.entity.Generator;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.implementations.TempestGit;

public class GitProtocollTesting {
    static String repo_name = "test_repo";
    static TempestGit peer_one, peer_two;
    
    // Test directories
    static Path start_files = Path.of("src/test/resources/start_files/");
    static Path add_files = Path.of("src/test/resources/add_files/");
    static Path after_added_files = Path.of("src/test/resources/after_added_files/");

    private static void check_files(Path path_one, Path path_two) throws Exception {
        File[] path_one_files = path_one.toFile().listFiles();
        File[] path_two_files = path_two.toFile().listFiles();

        assertEquals(path_one_files.length, path_two_files.length);

        for (int i = 0; i < path_one_files.length; i++) {
            assertEquals(Generator.md5_Of_File(path_one_files[i]), Generator.md5_Of_File(path_two_files[i]));
        }
    }

    @BeforeAll
    static void init() throws Exception {
        peer_one = new TempestGit(0, "127.0.0.1");
        peer_two = new TempestGit(1, "127.0.0.1");
    }

    @AfterEach
    void reset() {
        peer_one.leaveNetwork();
        peer_two.leaveNetwork();
    }

    @Test
    void testCase_CreateRepository(@TempDir Path temp_dir) throws Exception {
        assertTrue(peer_one.createRepository(repo_name, start_files, temp_dir));
    }

    @Test
    void testCase_CreateExistingRepo(@TempDir Path temp_dir) throws Exception {
        peer_one.createRepository(repo_name, start_files, temp_dir);
        assertThrows(RepositoryAlreadyExistException.class, () -> peer_one.createRepository(repo_name, start_files, temp_dir));
    }
}