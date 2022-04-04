import Implementation.AnonymousChatImpl;
import it.isislab.p2p.git.interfaces.MessageListener;
import beans.ChatRoom;
import beans.Message;
import it.isislab.p2p.git.interfaces.PublishSubscribe;

import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.beryx.textio.TextTerminal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AnonymousChatTesting {

    private static PublishSubscribe peer0, peer1, peer2, peer3;
    HashMap<String, List<Message>> listHashMap = new HashMap<>();

    public AnonymousChatTesting() throws Exception {
        class MessageListenerImpl implements MessageListener {

            int peerid;
            Message message = null;
            HashMap<String, List<Message>> listMsg = new HashMap<>();

            public MessageListenerImpl(int peerid) {
                this.peerid = peerid;

            }

            public Object parseMessage(Object obj) {
                this.message = (Message) obj;
                TextIO textIO = TextIoFactory.getTextIO();
                TextTerminal terminal = textIO.getTextTerminal();
                //Verifico se il messaggio è diverso da null ed ha una roomName valido
                if (message != null && message.getRoomName() != null) {
                    System.out.println("Sono dentro al null");
                    //Se la lista dei messaggi della room non è null, allora posso ottenere la lista e aggiungere un nuovo messaggio
                    if (listHashMap.get(message.getRoomName()) != null) {
                        System.out.println("Sono dentro al secondo null");
                        listHashMap.get(message.getRoomName()).add(message);//Aggiungo il messaggio
                        //terminal.printf("\n" + peerid + "] (Direct Message Received) Message received in room: " + message.getRoomName() + "ALLE ORE" + message.getData().toString() + "\n\n");
                    }
                }
                if (message != null) return message;
                return "not success";
            }

            public HashMap<String, List<Message>> getListMsg(String roomm) {
                return listMsg;
            }
        }


    }

    @BeforeAll
    static void inizializzaione() throws Exception {
        peer0 = new AnonymousChatImpl(0, "127.0.0.1", new MessageListenerImpl(0));
        peer1 = new AnonymousChatImpl(1, "127.0.0.1", new MessageListenerImpl(1));
        peer2 = new AnonymousChatImpl(2, "127.0.0.1", new MessageListenerImpl(2));
        peer3 = new AnonymousChatImpl(3, "127.0.0.1", new MessageListenerImpl(3));
    }

    //Test case per creazione della chatroom
    @Test
    void testCaseCreateChatRoom() {
        String ris = peer0.createChatRoom(new ChatRoom("1.1CreateRoom", new HashSet<>()));
        assertEquals("Successo", ris);
    }

    /*Test case per la creazione di una chatRoom già creata
        1. Un peer cerca di creare ma trova un messaggio di room già esistente
        2. Un peer cerca di creare una room ma riceve un messaggio di room alla quale è già joinato
    * */
    @Test
    void testCaseCreateRoomAlreadyJoinedorAlreadyCreated() {
        //Creo la stanza
        String ris1 = peer0.createChatRoom(new ChatRoom("1.2CreateRoom_AlreadyCreated", new HashSet<>()));
        assertEquals("Successo", ris1);
        //Provo a crearne un altra con lo stesso nome
        String ris2 = peer1.createChatRoom(new ChatRoom("1.2CreateRoom_AlreadyCreated", new HashSet<>()));
        assertEquals("Esistente", ris2);
        //Provo a creare una stanza dove sono già joinato
        String ris3 = peer0.createChatRoom(new ChatRoom("1.2CreateRoom_AlreadyCreated", new HashSet<>()));
        assertEquals("Esistente", ris3);

    }

    @Test
    void testCaseJoinRoom() {
        String ris1 = peer0.createChatRoom(new ChatRoom("2.1JoinRoom", new HashSet<>()));
        assertEquals("Successo", ris1);

        String ris2 = peer1.tryToJoinRoom("2.1JoinRoom");
        assertEquals("Successo", ris2);

        String ris3 = peer2.tryToJoinRoom("2.1JoinRoom");
        assertEquals("Successo", ris3);

        String ris4 = peer3.tryToJoinRoom("2.1JoinRoom");
        assertEquals("Successo", ris4);

    }

    @Test
    void testCaseJoinRoomAlreadyJoined() {

        //Peer0 crea la stanza
        String ris1 = peer0.createChatRoom(new ChatRoom("2.2JoinRoomAlreadyJoined", new HashSet<>()));
        assertEquals("Successo", ris1);

        //Peer1 effettua il join
        String ris2 = peer1.tryToJoinRoom("2.2JoinRoomAlreadyJoined");
        assertEquals("Successo", ris2);

        //Peer2 effettua il join
        String ris3 = peer2.tryToJoinRoom("2.2JoinRoomAlreadyJoined");
        assertEquals("Successo", ris3);

        //Peer 2 prova a rieffettuarlo anche se è già joinato
        String ris4 = peer2.tryToJoinRoom("2.2JoinRoomAlreadyJoined");
        assertEquals("Joined", ris4);

    }

    @Test
    void testCaseJoinRoomNotExistent() {
        String ris0 = peer1.tryToJoinRoom("2.3JoinRoomNotExistent");
        assertEquals("Fallimento", ris0);
    }

    @Test
    void testCaseLeaveRoom() throws IOException, ClassNotFoundException {

        //Peer0 effettua la creazione
        String ris1 = peer0.createChatRoom(new ChatRoom("3.1LeaveRoom", new HashSet<>()));
        assertEquals("Successo", ris1);

        //Peer1 effettua il join
        String ris2 = peer1.tryToJoinRoom("3.1LeaveRoom");
        assertEquals("Successo", ris2);

        //Peer0 cerca di uscire
        String ris3 = peer0.leaveRoom("3.1LeaveRoom");
        assertEquals("Leave", ris3);

        //Peer 2 prova a rieffettuarlo anche se è già joinato
        String ris4 = peer1.leaveRoom("3.1LeaveRoom");
        assertEquals("Leave", ris4);
    }

    @Test
    void testCaseLeaveRoomNotJoined() throws IOException, ClassNotFoundException {

        //Peer0 effettua la creazione
        String ris1 = peer0.createChatRoom(new ChatRoom("3.2LeaveRoomNotJoined", new HashSet<>()));
        assertEquals("Successo", ris1);

        //Peer1 effettua la leave nonostante non sia nella stanza
        String ris2 = peer1.leaveRoom("3.2LeaveRoomNotJoined");
        assertEquals("Not joined", ris2);
    }

    @Test
    void testCaseLeaveRoomNotCreated() throws IOException, ClassNotFoundException {

        //Peer0 effettua la leave di una stanza non creata
        String ris1 = peer0.leaveRoom("3.3LeaveRoomNotCreated");
        assertEquals("Not joined", ris1);

    }

    @Test
    void testCaseSendMsg() throws ClassNotFoundException {

        //Peer0 effettua la creazione
        String ris1 = peer0.createChatRoom(new ChatRoom("4.1SendMsg", new HashSet<>()));
        assertEquals("Successo", ris1);

        String ris2 = peer1.tryToJoinRoom("4.1SendMsg");
        assertEquals("Successo", ris2);

        String ris3 = peer2.tryToJoinRoom("4.1SendMsg");
        assertEquals("Successo", ris3);

        String ris4 = peer3.tryToJoinRoom("4.1SendMsg");
        assertEquals("Successo", ris4);

        Message msg = new Message("Default message","4.1SendMsg", Calendar.getInstance().getTime(), true);
        String risSend=peer1.tryToSendMsg("4.1SendMsg",msg);
        assertEquals("Sent",risSend);
    }

    @Test
    void testCaseSendMsgRoomNotJoined() throws ClassNotFoundException {
        String ris1=peer0.createChatRoom(new ChatRoom("4.2SendMsgNotJoined",new HashSet<>()));
        assertEquals("Successo",ris1);

        Message msg = new Message("Default message","4.2SendMsgNotJoined", Calendar.getInstance().getTime(), true);
        String risSend=peer1.tryToSendMsg("4.2SendMsgNotJoined",msg);
        assertEquals("Not in the room",risSend);
    }

    @Test
    void testCaseDestroyRoom() throws IOException, ClassNotFoundException {
        String ris1=peer0.createChatRoom(new ChatRoom("5.1DestroyRoom",new HashSet<>()));
        assertEquals("Successo",ris1);

        String ris2=peer0.destroyRoom("5.1DestroyRoom");
        assertEquals("Destroyed",ris2);
    }

    @Test
    void testCaseDestroyRoomWithMoreThan1Users() throws IOException, ClassNotFoundException {
        String ris1=peer0.createChatRoom(new ChatRoom("5.2DestroyRoomWithUsers",new HashSet<>()));
        assertEquals("Successo",ris1);

        String ris2=peer1.tryToJoinRoom("5.2DestroyRoomWithUsers");
        assertEquals("Successo",ris2);

        String ris3=peer0.destroyRoom("5.2DestroyRoomWithUsers");
        assertEquals("Not Destroyed",ris3);
    }

    @Test
    void testCaseDestroyRoomNotJoined() throws IOException, ClassNotFoundException {

        String ris3=peer0.destroyRoom("5.2DestroyRoomNotJoined");
        assertEquals("Not Found",ris3);
    }

    @Test
    void testCaseShowUsers() throws ClassNotFoundException {
        String ris1=peer0.createChatRoom(new ChatRoom("6.1ShowUsers",new HashSet<>()));
        assertEquals("Successo",ris1);

        String ris2=peer1.tryToJoinRoom("6.1ShowUsers");
        assertEquals("Successo",ris2);

        String risShow=peer0.showUsers("6.1ShowUsers");
        assertEquals("Founded",risShow);

    }
    @Test
    void testCaseShowUsersRoomNotJoined() throws ClassNotFoundException {

        String risShow=peer0.showUsers("6.2ShowUsersRoomNotFound");
        assertEquals("Not joined",risShow);
    }

    @Test
    void testCaseShowUsersAfterExit() throws ClassNotFoundException, IOException {
        String ris1 = peer0.createChatRoom(new ChatRoom("6.3ShowUsersAfterExit", new HashSet<>()));
        assertEquals("Successo", ris1);

        String risShow = peer0.showUsers("6.3ShowUsersAfterExit");
        assertEquals("Founded", risShow);

        String ris2 = peer1.tryToJoinRoom("6.3ShowUsersAfterExit");
        assertEquals("Successo", ris2);

        String ris2Show=peer1.showUsers("6.3ShowUsersAfterExit");
        assertEquals("Founded",ris2Show);

        String ris3 = peer1.leaveRoom("6.3ShowUsersAfterExit");
        assertEquals("Leave", ris3);

        String ris3Show=peer1.showUsers("6.3ShowUsersAfterExit");
        assertEquals("Not joined",ris3Show);

        String ris4 = peer0.leaveRoom("6.3ShowUsersAfterExit");
        assertEquals("Leave", ris4);

        String ris4Show=peer0.showUsers("6.3ShowUsersAfterExit");
        assertEquals("Not joined",ris3Show);


    }



    @AfterAll
    static void leaveNetwork() throws IOException, ClassNotFoundException {
        assertTrue(peer0.leaveNetwork());
        assertTrue(peer1.leaveNetwork());
        assertTrue(peer2.leaveNetwork());
        assertTrue(peer3.leaveNetwork());
    }
}