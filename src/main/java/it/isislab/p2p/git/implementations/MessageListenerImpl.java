package it.isislab.p2p.git.implementations;

import it.isislab.p2p.git.beans.Repository;
import it.isislab.p2p.git.interfaces.MessageListener;

public class MessageListenerImpl implements MessageListener{
    int peerid;

    public MessageListenerImpl(int peerid)
    {
        this.peerid = peerid;
    }
    
    public Object parseMessage(Object obj) {
        Repository repository = (Repository) obj;

        System.out.println("Repository " + repository.getName() + "updated. \n");

        return "success";
    }
}