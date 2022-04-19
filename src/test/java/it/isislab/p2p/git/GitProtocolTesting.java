package it.isislab.p2p.git;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.isislab.p2p.git.entity.Generator;
import it.isislab.p2p.git.exceptions.ConflictsNotResolvedException;
import it.isislab.p2p.git.exceptions.GeneratedConflictException;
import it.isislab.p2p.git.exceptions.NothingToPushException;
import it.isislab.p2p.git.exceptions.RepoStateChangedException;
import it.isislab.p2p.git.exceptions.RepositoryAlreadyExistException;
import it.isislab.p2p.git.exceptions.RepositoryNotExistException;
import it.isislab.p2p.git.implementations.TempestGit;

public class GitProtocolTesting {
    static TempestGit peer_one, peer_two;

    // Test directories
    static Path start_files = Path.of("src/test/resources/start_files/");
    static Path add_files = Path.of("src/test/resources/add_files/");
    static Path after_added_files = Path.of("src/test/resources/after_added_files/");
    static Path conflict_files = Path.of("src/test/resources/conflict_files/");

    // Test directories for peers
    static Path Peer_One_WD = Path.of("src/test/test_dir/Peer_One");
    static Path Peer_Two_WD = Path.of("src/test/test_dir/Peer_Two");

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
        peer_one = new TempestGit(0, "127.0.0.1", Peer_One_WD);
        peer_two = new TempestGit(1, "127.0.0.1", Peer_Two_WD);
    }

    @AfterAll
    static void reset() {
        peer_one.leaveNetwork();
        peer_two.leaveNetwork();

        peer_one = null;
        peer_two = null;

        File test_dir = new File("src/test/test_dir");
        try {
            FileUtils.deleteDirectory(test_dir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Get local repository tests
    // -----------------------------------------------------

    @Test
    void testCase_GetLocalRepository() {
        try {
            peer_one.createRepository("get_repo", start_files, Path.of("get_repo"));

            assertNotNull(peer_one.get_local_repo("get_repo"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_GetLocalRepositoryNoExist() {
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
    void testCase_GetRemoteRepository() {
        try {
            peer_one.createRepository("get_repo_remote", start_files, Path.of("get_repo_remote"));

            assertNotNull(peer_one.get_remote_repo("get_repo_remote"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_GetRemotelRepositoryNoExist() {
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
    void testCase_GetLocalCommits() {
        try {
            peer_one.createRepository("get_commits", start_files, Path.of("get_commits"));

            peer_one.addFilesToRepository("get_commits", add_files);

            peer_one.commit("get_commits", "Create a commit");

            assertNotNull(peer_one.get_local_commits("get_commits"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_GetLocalCommitsNoCommit() {
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
    void testCase_CreateRepository() {
        try {
            assertTrue(peer_one.createRepository("new_repo", start_files, Path.of("new_repo")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CreateRepository_FileTest() {
        try {
            assertTrue(peer_one.createRepository("new_files_repo", start_files, Path.of("new_files_repo")));

            check_files(start_files, Path.of(Peer_One_WD + "/" + "new_files_repo"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CreateExistingRepo() {
        try {
            peer_one.createRepository("new_existing_repo", start_files, Path.of("new_existing_repo"));
            assertThrows(RepositoryAlreadyExistException.class, () -> peer_two.createRepository("new_existing_repo", start_files, Path.of("new_existing_repo")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Clone repository tests
    // -----------------------------------------------------

    @Test
    void testCase_CloneRepository() {
        try {
            peer_one.createRepository("clone_repo", start_files, Path.of("clone_repo"));
            assertTrue(peer_two.clone("clone_repo", Path.of("clone_repo")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_CloneRepository_FileTest() {
        try {
            peer_one.createRepository("clone_file_repo", start_files, Path.of("clone_file_repo"));
            assertTrue(peer_two.clone("clone_file_repo", Path.of("clone_file_repo")));
            check_files(start_files, Path.of("clone_file_repo"));
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
    void testCase_AddFiles() {
        try {
            peer_one.createRepository("add_repo", start_files, Path.of("add_repo"));

            assertNotNull(peer_one.addFilesToRepository("add_repo", add_files));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_AddFilesNotExistingRepository() {
        try {
            assertThrows(RepositoryNotExistException.class, () -> peer_one.addFilesToRepository("add_repo_Not_Exist", add_files));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_AddFiles_FileTest() {
        try {
            peer_one.createRepository("add_repo_files", start_files, Path.of("add_repo_files"));

            assertNotNull(peer_one.addFilesToRepository("add_repo_files", add_files));

            peer_one.commit("add_repo_files", "Add files test");

            peer_one.push("add_repo_files");

            peer_one.pull("add_repo_files");

            check_files(Path.of(Peer_One_WD + "/"+ "add_repo_files"), after_added_files);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // // -----------------------------------------------------
    // // Commit tests
    // // -----------------------------------------------------

    @Test
    void testCase_Commit() {
        try {
            peer_one.createRepository("commit", start_files, Path.of("commit"));

            File file = new File(Path.of("commit") + "/test_file_one.txt");
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
    void testCase_EmptyCommit() {
        try {
            peer_one.createRepository("empty_commit", start_files, Path.of("empty_commit"));

            assertNull(peer_one.commit("empty_commit", "Empty Commit Message"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // -----------------------------------------------------
    // Push tests
    // -----------------------------------------------------

    @Test
    void testCase_Push() {
        try {
            peer_one.createRepository("push_repo", start_files, Path.of("push_repo"));

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
            peer_one.createRepository("push_repo_files", start_files, Path.of("push_repo_files"));

            peer_two.clone("push_repo_files", Path.of("push_repo_files"));

            peer_one.addFilesToRepository("push_repo_files", add_files);

            peer_one.commit("push_repo_files", "Push test");

            peer_one.push("push_repo_files");

            peer_one.pull("push_repo_files");

            peer_two.pull("push_repo_files");

            check_files(Path.of(Peer_One_WD + "/" + "push_repo_files"), Path.of(Peer_Two_WD + "/" + "push_repo_files"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PushToNotExistingRepo() {
        try {
            assertThrows(RepositoryNotExistException.class, () -> peer_one.push("push_not_exist"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PushNothingToPush() {
        try {
            peer_one.createRepository("push_nothing", start_files, Path.of("push_nothing"));

            assertThrows(NothingToPushException.class, () -> peer_one.push("push_nothing"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PushRepositoryStateChanged() {
        try {
            peer_one.createRepository("push_state_change", start_files, Path.of("push_state_change"));

            peer_two.clone("push_state_change", Path.of("push_state_change"));

            peer_one.addFilesToRepository("push_state_change", add_files);

            peer_one.commit("push_state_change", "Add files");

            peer_one.push("push_state_change");

            File file = new File(Peer_Two_WD + "/push_state_change/test_file_one.txt");
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
    void testCase_Pull() {
        try {
            peer_one.createRepository("repo_pull", start_files, Path.of("repo_pull"));

            peer_two.clone("repo_pull", Path.of("repo_pull"));

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
            peer_one.createRepository("repo_pull_test_file", start_files, Path.of("repo_pull_test_file"));

            peer_two.clone("repo_pull_test_file", Path.of("repo_pull_test_file"));

            peer_one.addFilesToRepository("repo_pull_test_file", add_files);

            peer_one.commit("repo_pull_test_file", "added files");

            peer_one.push("repo_pull_test_file");

            peer_one.pull("repo_pull_test_file");

            peer_two.pull("repo_pull_test_file");

            check_files(Path.of(Peer_One_WD + "/repo_pull_test_file"), Path.of(Peer_Two_WD + "/repo_pull_test_file"));
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
            peer_one.createRepository("conflict_repo", start_files, Path.of("conflict_repo"));

            peer_two.clone("conflict_repo", temp_dir_two);

            peer_one.addFilesToRepository("conflict_repo", add_files);

            peer_one.commit("conflict_repo", "added files");

            peer_one.push("conflict_repo");

            File file = new File(Peer_Two_WD + "conflict_repo/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertThrows(GeneratedConflictException.class, () -> peer_two.pull("conflict_repo"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    void testCase_PullConflictResolved() {
        try {
            peer_one.createRepository("conflict_repo_resolved", start_files, Path.of("conflict_repo_resolved"));

            peer_two.clone("conflict_repo_resolved", Path.of("conflict_repo_resolved"));

            peer_one.addFilesToRepository("conflict_repo_resolved", add_files);

            peer_one.commit("conflict_repo_resolved", "added files");

            peer_one.push("conflict_repo_resolved");

            File file = new File(Peer_Two_WD + "/conflict_repo_resolved/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertThrows(GeneratedConflictException.class, () -> peer_two.pull("conflict_repo_resolved"));

            File maintained_file = new File(Peer_Two_WD + "/conflict_repo_resolved/test_file_one.txt");
            File remote_file = new File(Peer_Two_WD + "/conflict_repo_resolved/REMOTE-test_file_one.txt");
            File local_file = new File(Peer_Two_WD + "/conflict_repo_resolved/LOCAL-test_file_one.txt");
            local_file.renameTo(maintained_file);
            remote_file.delete();

            assertTrue(peer_two.pull("conflict_repo_resolved"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullConflictResolved_TestFile() {
        try {
            peer_one.createRepository("conflict_repo_resolved_test_file", start_files, Path.of("conflict_repo_resolved_test_file"));

            peer_two.clone("conflict_repo_resolved_test_file", Path.of("conflict_repo_resolved_test_file"));

            peer_one.addFilesToRepository("conflict_repo_resolved_test_file", add_files);

            peer_one.commit("conflict_repo_resolved_test_file", "added files");

            peer_one.push("conflict_repo_resolved_test_file");

            File file = new File(Peer_Two_WD + "/conflict_repo_resolved_test_file/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("Ma qui prima c'era una papera 😢");
            bw.close();

            assertThrows(GeneratedConflictException.class, () -> peer_two.pull("conflict_repo_resolved_test_file"));

            File maintained_file = new File(Peer_Two_WD + "/conflict_repo_resolved_test_file/test_file_one.txt");
            File remote_file = new File(Peer_Two_WD + "/conflict_repo_resolved_test_file/REMOTE-test_file_one.txt");
            File local_file = new File(Peer_Two_WD + "/conflict_repo_resolved_test_file/LOCAL-test_file_one.txt");
            local_file.renameTo(maintained_file);
            remote_file.delete();

            assertTrue(peer_two.pull("conflict_repo_resolved_test_file"));

            peer_two.push("conflict_repo_resolved_test_file");

            peer_one.pull("conflict_repo_resolved_test_file");

            check_files(Path.of(Peer_One_WD + "/conflict_repo_resolved_test_file"), conflict_files);

            peer_one.commit("conflict_repo_resolved_test_file", "all_resolved");

            peer_one.push("conflict_repo_resolved_test_file");

            peer_two.pull("conflict_repo_resolved_test_file");

            check_files(Path.of(Peer_Two_WD + "/conflict_repo_resolved_test_file"), conflict_files);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullConflictNotResolved() {
        try {
            peer_one.createRepository("conflict_repo_not_resolved", start_files, Path.of("conflict_repo_not_resolved"));

            peer_two.clone("conflict_repo_not_resolved", Path.of("conflict_repo_not_resolved"));

            peer_one.addFilesToRepository("conflict_repo_not_resolved", add_files);

            peer_one.commit("conflict_repo_not_resolved", "added files");

            peer_one.push("conflict_repo_not_resolved");

            File file = new File(Peer_Two_WD + "/conflict_repo_not_resolved/test_file_one.txt");
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("modification");
            bw.close();

            assertThrows(GeneratedConflictException.class, () -> peer_two.pull("conflict_repo_not_resolved"));

            assertThrows(ConflictsNotResolvedException.class, () -> peer_two.pull("conflict_repo_not_resolved"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testCase_PullConflictNotResolved_WhitAnotherConflict() {
        try {
            peer_one.createRepository("conflict_repo_not_resolved_with_another", start_files, Path.of("conflict_repo_not_resolved_with_another"));

            peer_two.clone("conflict_repo_not_resolved_with_another", Path.of("conflict_repo_not_resolved_with_another"));

            peer_one.addFilesToRepository("conflict_repo_not_resolved_with_another", add_files);

            peer_one.commit("conflict_repo_not_resolved_with_another", "added files");

            peer_one.push("conflict_repo_not_resolved_with_another");

            File file_one = new File(Peer_Two_WD + "/conflict_repo_not_resolved_with_another/test_file_one.txt");
            FileWriter fw_one = new FileWriter(file_one.getAbsoluteFile());
            BufferedWriter bw_one = new BufferedWriter(fw_one);
            bw_one.write("modification");
            bw_one.close();

            assertThrows(GeneratedConflictException.class, () -> peer_two.pull("conflict_repo_not_resolved_with_another"));

            File file_two = new File(Peer_Two_WD + "/conflict_repo_not_resolved_with_another/test_file_two.txt");
            FileWriter fw_two = new FileWriter(file_two.getAbsoluteFile());
            BufferedWriter bw_two = new BufferedWriter(fw_two);
            bw_two.write("modification");
            bw_two.close();

            assertThrows(GeneratedConflictException.class, () -> peer_two.pull("conflict_repo_not_resolved_with_another"));

            assertThrows(ConflictsNotResolvedException.class, () -> peer_two.pull("conflict_repo_not_resolved_with_another"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}