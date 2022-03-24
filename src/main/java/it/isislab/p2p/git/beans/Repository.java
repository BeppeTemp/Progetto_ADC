package it.isislab.p2p.git.beans;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.tomp2p.peers.PeerAddress;

public class Repository implements Serializable {
    private ArrayList<File> files;
    private HashSet<PeerAddress> users;

    // Costruttore
    public Repository(File directory, HashSet<PeerAddress> users) {
        Collections.addAll(this.files, directory.listFiles());
        this.users = users;
    }

    // Getter & Setter
    public ArrayList<File> getFiles() {
        return this.files;
    }

    public void setFiles(ArrayList<File> files) {
        this.files = files;
    }

    public HashSet<PeerAddress> getUsers() {
        return this.users;
    }

    public void setUsers(HashSet<PeerAddress> users) {
        this.users = users;
    }

}