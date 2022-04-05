package it.isislab.p2p.git;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import it.isislab.p2p.git.classes.Repository;
import it.isislab.p2p.git.implementations.GitProtocolImpl;
import it.isislab.p2p.git.interfaces.GitProtocol;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class GitProtocollTesting {
    private static GitProtocol peer_one, peer_two, peer_three, peer_four;
    private static File start_files, add_files, after_added_directory;

    @TempDir
    static Path repo_one;

    private static String repo_one_name;

    public GitProtocollTesting() throws Exception {
        repo_one_name = "Repo_One";

        start_files = new File("src/test/resources/start_files");
        add_files = new File("src/test/resources/add_files");
        after_added_directory = new File("src/test/resources/after_added_files");
    }

    // @BeforeAll
    // static void init() throws Exception {
    //     peer_one = new GitProtocolImpl(0, "127.0.0.1");
    //     peer_two = new GitProtocolImpl(1, "127.0.0.1");
    //     peer_three = new GitProtocolImpl(2, "127.0.0.1");
    //     peer_four = new GitProtocolImpl(3, "127.0.0.1");

    //     File repo = repo_one.resolve(repo_one_name + "/").toFile();
    //     peer_one.createRepository(repo_one_name, start_files);

    //     assertEquals(3, repo.listFiles().length);
    // }

    // @Test
    // void testCaseCreateRepository() {
    //     assertArrayEquals(start_files.listFiles(), start_files.listFiles());

    //     System.out.println("ciao");

    //     assertEquals(3, start_files.listFiles().length);
    // }

    // @Test
    // void testCaseAddFiles() {
    //     peer_one.addFilesToRepository(repo_one_name, add_files.listFiles());
    //     assertEquals(repo_one.listFiles().length, after_added_directory.listFiles().length);
    // }

    // /*Test case per la creazione di una chatRoom già creata
    // 1. Un peer cerca di creare ma trova un messaggio di room già esistente
    // 2. Un peer cerca di creare una room ma riceve un messaggio di room alla quale
    // è già joinato
    // * */
    // @Test
    // void testCaseCreateRoomAlreadyJoinedorAlreadyCreated() {
    // //Creo la stanza
    // String ris1 = peer0.createChatRoom(new
    // ChatRoom("1.2CreateRoom_AlreadyCreated", new HashSet<>()));
    // assertEquals("Successo", ris1);
    // //Provo a crearne un altra con lo stesso nome
    // String ris2 = peer1.createChatRoom(new
    // ChatRoom("1.2CreateRoom_AlreadyCreated", new HashSet<>()));
    // assertEquals("Esistente", ris2);
    // //Provo a creare una stanza dove sono già joinato
    // String ris3 = peer0.createChatRoom(new
    // ChatRoom("1.2CreateRoom_AlreadyCreated", new HashSet<>()));
    // assertEquals("Esistente", ris3);

    // }

    // @Test
    // void testCaseJoinRoom() {
    // String ris1 = peer0.createChatRoom(new ChatRoom("2.1JoinRoom", new
    // HashSet<>()));
    // assertEquals("Successo", ris1);

    // String ris2 = peer1.tryToJoinRoom("2.1JoinRoom");
    // assertEquals("Successo", ris2);

    // String ris3 = peer2.tryToJoinRoom("2.1JoinRoom");
    // assertEquals("Successo", ris3);

    // String ris4 = peer3.tryToJoinRoom("2.1JoinRoom");
    // assertEquals("Successo", ris4);

    // }

    // @Test
    // void testCaseJoinRoomAlreadyJoined() {

    // //Peer0 crea la stanza
    // String ris1 = peer0.createChatRoom(new ChatRoom("2.2JoinRoomAlreadyJoined",
    // new HashSet<>()));
    // assertEquals("Successo", ris1);

    // //Peer1 effettua il join
    // String ris2 = peer1.tryToJoinRoom("2.2JoinRoomAlreadyJoined");
    // assertEquals("Successo", ris2);

    // //Peer2 effettua il join
    // String ris3 = peer2.tryToJoinRoom("2.2JoinRoomAlreadyJoined");
    // assertEquals("Successo", ris3);

    // //Peer 2 prova a rieffettuarlo anche se è già joinato
    // String ris4 = peer2.tryToJoinRoom("2.2JoinRoomAlreadyJoined");
    // assertEquals("Joined", ris4);

    // }

    // @Test
    // void testCaseJoinRoomNotExistent() {
    // String ris0 = peer1.tryToJoinRoom("2.3JoinRoomNotExistent");
    // assertEquals("Fallimento", ris0);
    // }

    // @Test
    // void testCaseLeaveRoom() throws IOException, ClassNotFoundException {

    // //Peer0 effettua la creazione
    // String ris1 = peer0.createChatRoom(new ChatRoom("3.1LeaveRoom", new
    // HashSet<>()));
    // assertEquals("Successo", ris1);

    // //Peer1 effettua il join
    // String ris2 = peer1.tryToJoinRoom("3.1LeaveRoom");
    // assertEquals("Successo", ris2);

    // //Peer0 cerca di uscire
    // String ris3 = peer0.leaveRoom("3.1LeaveRoom");
    // assertEquals("Leave", ris3);

    // //Peer 2 prova a rieffettuarlo anche se è già joinato
    // String ris4 = peer1.leaveRoom("3.1LeaveRoom");
    // assertEquals("Leave", ris4);
    // }

    // @Test
    // void testCaseLeaveRoomNotJoined() throws IOException, ClassNotFoundException
    // {

    // //Peer0 effettua la creazione
    // String ris1 = peer0.createChatRoom(new ChatRoom("3.2LeaveRoomNotJoined", new
    // HashSet<>()));
    // assertEquals("Successo", ris1);

    // //Peer1 effettua la leave nonostante non sia nella stanza
    // String ris2 = peer1.leaveRoom("3.2LeaveRoomNotJoined");
    // assertEquals("Not joined", ris2);
    // }

    // @Test
    // void testCaseLeaveRoomNotCreated() throws IOException, ClassNotFoundException
    // {

    // //Peer0 effettua la leave di una stanza non creata
    // String ris1 = peer0.leaveRoom("3.3LeaveRoomNotCreated");
    // assertEquals("Not joined", ris1);

    // }

    // @Test
    // void testCaseSendMsg() throws ClassNotFoundException {

    // //Peer0 effettua la creazione
    // String ris1 = peer0.createChatRoom(new ChatRoom("4.1SendMsg", new
    // HashSet<>()));
    // assertEquals("Successo", ris1);

    // String ris2 = peer1.tryToJoinRoom("4.1SendMsg");
    // assertEquals("Successo", ris2);

    // String ris3 = peer2.tryToJoinRoom("4.1SendMsg");
    // assertEquals("Successo", ris3);

    // String ris4 = peer3.tryToJoinRoom("4.1SendMsg");
    // assertEquals("Successo", ris4);

    // Message msg = new Message("Default message","4.1SendMsg",
    // Calendar.getInstance().getTime(), true);
    // String risSend=peer1.tryToSendMsg("4.1SendMsg",msg);
    // assertEquals("Sent",risSend);
    // }

    // @Test
    // void testCaseSendMsgRoomNotJoined() throws ClassNotFoundException {
    // String ris1=peer0.createChatRoom(new ChatRoom("4.2SendMsgNotJoined",new
    // HashSet<>()));
    // assertEquals("Successo",ris1);

    // Message msg = new Message("Default message","4.2SendMsgNotJoined",
    // Calendar.getInstance().getTime(), true);
    // String risSend=peer1.tryToSendMsg("4.2SendMsgNotJoined",msg);
    // assertEquals("Not in the room",risSend);
    // }

    // @Test
    // void testCaseDestroyRoom() throws IOException, ClassNotFoundException {
    // String ris1=peer0.createChatRoom(new ChatRoom("5.1DestroyRoom",new
    // HashSet<>()));
    // assertEquals("Successo",ris1);

    // String ris2=peer0.destroyRoom("5.1DestroyRoom");
    // assertEquals("Destroyed",ris2);
    // }

    // @Test
    // void testCaseDestroyRoomWithMoreThan1Users() throws IOException,
    // ClassNotFoundException {
    // String ris1=peer0.createChatRoom(new ChatRoom("5.2DestroyRoomWithUsers",new
    // HashSet<>()));
    // assertEquals("Successo",ris1);

    // String ris2=peer1.tryToJoinRoom("5.2DestroyRoomWithUsers");
    // assertEquals("Successo",ris2);

    // String ris3=peer0.destroyRoom("5.2DestroyRoomWithUsers");
    // assertEquals("Not Destroyed",ris3);
    // }

    // @Test
    // void testCaseDestroyRoomNotJoined() throws IOException,
    // ClassNotFoundException {

    // String ris3=peer0.destroyRoom("5.2DestroyRoomNotJoined");
    // assertEquals("Not Found",ris3);
    // }

    // @Test
    // void testCaseShowUsers() throws ClassNotFoundException {
    // String ris1=peer0.createChatRoom(new ChatRoom("6.1ShowUsers",new
    // HashSet<>()));
    // assertEquals("Successo",ris1);

    // String ris2=peer1.tryToJoinRoom("6.1ShowUsers");
    // assertEquals("Successo",ris2);

    // String risShow=peer0.showUsers("6.1ShowUsers");
    // assertEquals("Founded",risShow);

    // }
    // @Test
    // void testCaseShowUsersRoomNotJoined() throws ClassNotFoundException {

    // String risShow=peer0.showUsers("6.2ShowUsersRoomNotFound");
    // assertEquals("Not joined",risShow);
    // }

    // @Test
    // void testCaseShowUsersAfterExit() throws ClassNotFoundException, IOException
    // {
    // String ris1 = peer0.createChatRoom(new ChatRoom("6.3ShowUsersAfterExit", new
    // HashSet<>()));
    // assertEquals("Successo", ris1);

    // String risShow = peer0.showUsers("6.3ShowUsersAfterExit");
    // assertEquals("Founded", risShow);

    // String ris2 = peer1.tryToJoinRoom("6.3ShowUsersAfterExit");
    // assertEquals("Successo", ris2);

    // String ris2Show=peer1.showUsers("6.3ShowUsersAfterExit");
    // assertEquals("Founded",ris2Show);

    // String ris3 = peer1.leaveRoom("6.3ShowUsersAfterExit");
    // assertEquals("Leave", ris3);

    // String ris3Show=peer1.showUsers("6.3ShowUsersAfterExit");
    // assertEquals("Not joined",ris3Show);

    // String ris4 = peer0.leaveRoom("6.3ShowUsersAfterExit");
    // assertEquals("Leave", ris4);

    // String ris4Show=peer0.showUsers("6.3ShowUsersAfterExit");
    // assertEquals("Not joined",ris3Show);

    // }

    // @AfterAll
    // static void leaveNetwork() throws IOException, ClassNotFoundException {
    // assertTrue(peer0.leaveNetwork());
    // assertTrue(peer1.leaveNetwork());
    // assertTrue(peer2.leaveNetwork());
    // assertTrue(peer3.leaveNetwork());
    // }
}