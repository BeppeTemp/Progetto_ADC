package it.isislab.p2p.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.isislab.p2p.git.entity.Generator;
import it.isislab.p2p.git.exceptions.ConflictsNotResolvedException;
import it.isislab.p2p.git.exceptions.GeneratedConflitException;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExistException;
import it.isislab.p2p.git.implementations.TempestGit;

public class GitProtocollTesting {
    static TempestGit peer_one, peer_two;

    // Test directories
    static Path start_files = Path.of("src/test/resources/start_files/");
    static Path add_files = Path.of("src/test/resources/add_files/");
    static Path after_added_files = Path.of("src/test/resources/after_added_files/");
    static Path conflict_files = Path.of("src/test/resources/conflict_files/");

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

    @AfterAll
    static void reset() {
        peer_one.leaveNetwork();
        peer_two.leaveNetwork();

        peer_one = null;
        peer_two = null;
    }

    // -----------------------------------------------------
    // Get local repository tests
    // -----------------------------------------------------

    @Test
    void testCase_GetLocalRepository(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("get_repo", start_files, temp_dir);

            assertNotNull(peer_one.get_local_repo("get_repo"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_GetLocalRepositoryNoExist(@TempDir Path temp_dir) {
        try {
            assertNull(peer_one.get_local_repo("get_repo_no_exist"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Get remote repository tests
    // -----------------------------------------------------

    @Test
    void testCase_GetRemoteRepository(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("get_repo_remote", start_files, temp_dir);

            assertNotNull(peer_one.get_remote_repo("get_repo_remote"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_GetRemotelRepositoryNoExist(@TempDir Path temp_dir) {
        try {
            assertNull(peer_one.get_remote_repo("get_repo_remote_no_exist"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Get local commits
    // -----------------------------------------------------
    @Test
    void testCase_GetLocalCommits(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("get_commits", start_files, temp_dir);

            peer_one.addFilesToRepository("get_commits", add_files);

            peer_one.commit("get_commits", "Create a commit");

            assertNotNull(peer_one.get_local_commits("get_commits"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_GetLocalCommitsNoCommit(@TempDir Path temp_dir) {
        try {
            assertNull(peer_one.get_local_commits("get_commits_no_commit"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Create repository tests
    // -----------------------------------------------------

    @Test
    void testCase_CreateRepository(@TempDir Path temp_dir) {
        try {
            assertTrue(peer_one.createRepository("new_repo", start_files, temp_dir));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CreateRepository_FileTest(@TempDir Path temp_dir) {
        try {
            assertTrue(peer_one.createRepository("new_files_repo", start_files, temp_dir));

            check_files(start_files, temp_dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CreateExistingRepo(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("new_existing_repo", start_files, temp_dir);
            assertThrows(RepositoryAlreadyExistException.class, () -> peer_two.createRepository("new_existing_repo", start_files, temp_dir));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Clone repository tests
    // -----------------------------------------------------

    @Test
    void testCase_CloneRepository(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("clone_repo", start_files, temp_dir_one);
            assertTrue(peer_two.clone("clone_repo", temp_dir_two));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CloneRepository_FileTest(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("clone_file_repo", start_files, temp_dir_one);
            assertTrue(peer_two.clone("clone_file_repo", temp_dir_two));
            check_files(start_files, temp_dir_two);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CloneNotExistingRepository(@TempDir Path temp_dir_one) {
        try {
            assertThrows(RepositoryNotExistException.class, () -> peer_one.clone("clone_nothing", temp_dir_one));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Add file tests
    // -----------------------------------------------------

    @Test
    void testCase_AddFiles(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("add_repo", start_files, temp_dir);

            assertNotNull(peer_one.addFilesToRepository("add_repo", add_files));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_AddFilesNotExistingRepository(@TempDir Path temp_dir) {
        try {
            assertThrows(RepositoryNotExistException.class, () -> peer_one.addFilesToRepository("add_repo_Not_Exist", add_files));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_AddFiles_FileTest(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("add_repo_files", start_files, temp_dir);

            assertNotNull(peer_one.addFilesToRepository("add_repo_files", add_files));

            peer_one.commit("add_repo_files", "Add files test");

            peer_one.push("add_repo_files");

            peer_one.pull("add_repo_files");

            check_files(temp_dir, after_added_files);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Commit tests
    // -----------------------------------------------------

    @Test
    void testCase_Commit(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("commit", start_files, temp_dir);

            File file = new File(temp_dir + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertNotNull(peer_one.commit("commit", "Commit Message"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_EmptyCommit(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("empty_commit", start_files, temp_dir);

            assertNull(peer_one.commit("empty_commit", "Empty Commit Message"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Push tests
    // -----------------------------------------------------

    @Test
    void testCase_Push(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("push_repo", start_files, temp_dir);

            peer_one.addFilesToRepository("push_repo", add_files);

            peer_one.commit("push_repo", "Push test");

            assertTrue(peer_one.push("push_repo"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_Push_TestFile(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("push_repo_files", start_files, temp_dir_one);

            peer_two.clone("push_repo_files", temp_dir_two);

            peer_one.addFilesToRepository("push_repo_files", add_files);

            peer_one.commit("push_repo_files", "Push test");

            peer_one.push("push_repo_files");

            peer_one.pull("push_repo_files");

            peer_two.pull("push_repo_files");

            check_files(temp_dir_one, temp_dir_two);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PushToNotExistingRepo(@TempDir Path temp_dir) {
        try {
            assertThrows(RepositoryNotExistException.class, () -> peer_one.push("push_not_exist"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PushNothingToPush(@TempDir Path temp_dir) {
        try {
            peer_one.createRepository("push_nothing", start_files, temp_dir);

            assertThrows(NothingToPushException.class, () -> peer_one.push("push_nothing"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PushRepositoryStateChanged(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("push_state_change", start_files, temp_dir_one);

            peer_two.clone("push_state_change", temp_dir_two);

            peer_one.addFilesToRepository("push_state_change", add_files);

            peer_one.commit("push_state_change", "Add files");

            peer_one.push("push_state_change");

            File file = new File(temp_dir_two + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            peer_two.commit("push_state_change", "Modify one file");

            assertThrows(RepoStateChangedException.class, () -> peer_two.push("push_state_change"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Pull tests
    // -----------------------------------------------------

    @Test
    void testCase_Pull(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("repo_pull", start_files, temp_dir_one);

            peer_two.clone("repo_pull", temp_dir_two);

            peer_one.addFilesToRepository("repo_pull", add_files);

            peer_one.commit("repo_pull", "added files");

            peer_one.push("repo_pull");

            assertTrue(peer_two.pull("repo_pull"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_Pull_TestFile(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("repo_pull_test_file", start_files, temp_dir_one);

            peer_two.clone("repo_pull_test_file", temp_dir_two);

            peer_one.addFilesToRepository("repo_pull_test_file", add_files);

            peer_one.commit("repo_pull_test_file", "added files");

            peer_one.push("repo_pull_test_file");

            peer_one.pull("repo_pull_test_file");

            peer_two.pull("repo_pull_test_file");

            check_files(temp_dir_one, temp_dir_two);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullRepositoryNotExist() {
        assertThrows(RepositoryNotExistException.class, () -> peer_one.pull("pull_not_exist"));
    }

    @Test
    void testCase_PullConflict(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("conflit_repo", start_files, temp_dir_one);

            peer_two.clone("conflit_repo", temp_dir_two);

            peer_one.addFilesToRepository("conflit_repo", add_files);

            peer_one.commit("conflit_repo", "added files");

            peer_one.push("conflit_repo");

            File file = new File(temp_dir_two + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertThrows(GeneratedConflitException.class, () -> peer_two.pull("conflit_repo"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    void testCase_PullConflictResolved(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("conflit_repo_resolved", start_files, temp_dir_one);

            peer_two.clone("conflit_repo_resolved", temp_dir_two);

            peer_one.addFilesToRepository("conflit_repo_resolved", add_files);

            peer_one.commit("conflit_repo_resolved", "added files");

            peer_one.push("conflit_repo_resolved");

            File file = new File(temp_dir_two + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertThrows(GeneratedConflitException.class, () -> peer_two.pull("conflit_repo_resolved"));

            File mainteined_file = new File(temp_dir_two + "/test_file_one.txt");
            File remote_file = new File(temp_dir_two + "/REMOTE-test_file_one.txt");
            File local_file = new File(temp_dir_two + "/LOCAL-test_file_one.txt");
            local_file.renameTo(mainteined_file);
            remote_file.delete();

            assertTrue(peer_two.pull("conflit_repo_resolved"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullConflictResolved_TestFile(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("conflit_repo_resolved_test_file", start_files, temp_dir_one);

            peer_two.clone("conflit_repo_resolved_test_file", temp_dir_two);

            peer_one.addFilesToRepository("conflit_repo_resolved_test_file", add_files);

            peer_one.commit("conflit_repo_resolved_test_file", "added files");

            peer_one.push("conflit_repo_resolved_test_file");

            File file = new File(temp_dir_two + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("Ma qui prima c'era una papera 😢");
            bw.close();

            assertThrows(GeneratedConflitException.class, () -> peer_two.pull("conflit_repo_resolved_test_file"));

            File mainteined_file = new File(temp_dir_two + "/test_file_one.txt");
            File remote_file = new File(temp_dir_two + "/REMOTE-test_file_one.txt");
            File local_file = new File(temp_dir_two + "/LOCAL-test_file_one.txt");
            local_file.renameTo(mainteined_file);
            remote_file.delete();

            assertTrue(peer_two.pull("conflit_repo_resolved_test_file"));

            peer_two.push("conflit_repo_resolved_test_file");

            peer_one.pull("conflit_repo_resolved_test_file");

            check_files(temp_dir_one, conflict_files);

            peer_one.commit("conflit_repo_resolved_test_file", "all_resolved");

            peer_one.push("conflit_repo_resolved_test_file");

            peer_two.pull("conflit_repo_resolved_test_file");

            check_files(temp_dir_two, conflict_files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullConflictNotResolved(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("conflit_repo_not_resolved", start_files, temp_dir_one);

            peer_two.clone("conflit_repo_not_resolved", temp_dir_two);

            peer_one.addFilesToRepository("conflit_repo_not_resolved", add_files);

            peer_one.commit("conflit_repo_not_resolved", "added files");

            peer_one.push("conflit_repo_not_resolved");

            File file = new File(temp_dir_two + "/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertThrows(GeneratedConflitException.class, () -> peer_two.pull("conflit_repo_not_resolved"));

            assertThrows(ConflictsNotResolvedException.class, () -> peer_two.pull("conflit_repo_not_resolved"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullConflictNotResolved_WhithAnotherConflict(@TempDir Path temp_dir_one, @TempDir Path temp_dir_two) {
        try {
            peer_one.createRepository("conflit_repo_not_resolved_with_another", start_files, temp_dir_one);

            peer_two.clone("conflit_repo_not_resolved_with_another", temp_dir_two);

            peer_one.addFilesToRepository("conflit_repo_not_resolved_with_another", add_files);

            peer_one.commit("conflit_repo_not_resolved_with_another", "added files");

            peer_one.push("conflit_repo_not_resolved_with_another");

            File file_one = new File(temp_dir_two + "/test_file_one.txt");
            FileWriter fw_one = new FileWriter(file_one.getAbsoluteFile());
            BufferedWriter bw_one = new BufferedWriter(fw_one);
            bw_one.write("modification");
            bw_one.close();

            assertThrows(GeneratedConflitException.class, () -> peer_two.pull("conflit_repo_not_resolved_with_another"));

            File file_two = new File(temp_dir_two + "/test_file_two.txt");
            FileWriter fw_two = new FileWriter(file_two.getAbsoluteFile());
            BufferedWriter bw_two = new BufferedWriter(fw_two);
            bw_two.write("modification");
            bw_two.close();

            assertThrows(GeneratedConflitException.class, () -> peer_two.pull("conflit_repo_not_resolved_with_another"));

            assertThrows(ConflictsNotResolvedException.class, () -> peer_two.pull("conflit_repo_not_resolved_with_another"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}