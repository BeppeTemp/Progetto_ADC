package it.isislab.p2p.git.beans;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import net.tomp2p.peers.PeerAddress;

public class Repository implements Serializable {
    private String name;
    private ArrayList<File> files;
    private HashSet<PeerAddress> users;
    private boolean modified;

    // Costruttore
    public Repository(String name, File directory, HashSet<PeerAddress> users) {
        this.name = name;
        this.files = new ArrayList<File>();
        Collections.addAll(this.files, directory.listFiles());
        this.users = users;
        this.modified = false;
    }

    // Metodo per l'aggiunta di file alla repository
    public void add_Files(ArrayList<File> new_files) {
        for (File file : new_files) {
            if (!this.contains_File(file))
                this.files.add(file);
        }
        this.modified = true;
    }

    // Controlla se il file è già presente nella repository
    public boolean contains_File(File file_to_check) {
        for (File file : this.files) {
            if(file_to_check.getName().compareTo(file.getName()) == 0)
                return true;
        }
        return false;
    }

    // Getter & Setter
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public boolean isModified() {
        return this.modified;
    }

    public boolean getModified() {
        return this.modified;
    }

    public void setModified(boolean modified) {
        this.modified = modified;
    }
}