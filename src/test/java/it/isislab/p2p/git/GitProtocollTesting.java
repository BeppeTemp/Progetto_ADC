package it.isislab.p2p.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.isislab.p2p.git.entity.Generator;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.implementations.TempestGit;

public class GitProtocollTesting {
    static TempestGit peer_one, peer_two, peer_three;

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
        peer_three = new TempestGit(2, "127.0.0.1");
    }

    @AfterAll
    static void reset() {
        peer_one.leaveNetwork();
        peer_two.leaveNetwork();

        peer_one = null;
        peer_two = null;
    }

    @Test
    void testCase_CreateRepository(@TempDir Path temp_dir) {
        try {
            assertTrue(peer_one.createRepository("new_repo", start_files, temp_dir));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_CreateRepositoryFileTest(@TempDir Path temp_dir) {
        try {
            assertTrue(peer_one.createRepository("new_files_repo", start_files, temp_dir));

            check_files(start_files, temp_dir);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_CreateExistingRepo(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("new_existing_repo", start_files, temp_dir);
            assertThrows(RepositoryAlreadyExistException.class, () -> peer_two.createRepository("new_existing_repo", start_files, temp_dir));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_CloneRepository(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("clone_repo", start_files, temp_dir_one);
            assertTrue(peer_two.clone("clone_repo", temp_dir_two));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_CloneRepositoryFileTest(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("clone_file_repo", start_files, temp_dir_one);
            assertTrue(peer_two.clone("clone_file_repo", temp_dir_two));
            check_files(start_files, temp_dir_two);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_AddFiles(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("add_repo", start_files, temp_dir);

            assertNotNull(peer_one.addFilesToRepository("add_repo", add_files));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_AddFilesFileTest(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("add_repo_files", start_files, temp_dir);

            assertNotNull(peer_one.addFilesToRepository("add_repo_files", add_files));

            peer_one.commit("add_repo_files", "Add files test");

            peer_one.push("add_repo_files");

            peer_one.pull("add_repo_files");

            check_files(temp_dir, after_added_files);

        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_Commit(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("commit", start_files, temp_dir);

            File file = new File(temp_dir + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertTrue(peer_one.commit("commit", "Commit Message"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_EmptyCommit(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("empty_commit", start_files, temp_dir);

            assertFalse(peer_three.commit("empty_commit", "Empty Commit Message"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_Push(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("push_repo", start_files, temp_dir);

            peer_one.addFilesToRepository("push_repo", add_files);

            peer_one.commit("push_repo", "Push test");

            assertTrue(peer_three.push("push_repo"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    void testCase_Pull(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("empty_commit", start_files, temp_dir);

            assertFalse(peer_three.commit("empty_commit", "Empty Commit Message"));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}