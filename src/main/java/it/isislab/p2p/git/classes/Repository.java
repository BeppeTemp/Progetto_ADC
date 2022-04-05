package it.isislab.p2p.git.classes;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.tomp2p.peers.PeerAddress;

public class Repository implements Serializable {
    private String name;
    private int version;
    private HashSet<PeerAddress> users;
    private HashMap<String, Item> items;
    private ArrayList<Commit> commits;

    // Costruttore
    public Repository(String name, HashSet<PeerAddress> users, Path start_dir) throws Exception {
        this.name = name;
        this.users = users;

        this.items = new HashMap<String, Item>();
        this.commits = new ArrayList<Commit>();

        this.version = this.commits.size();

        File[] files = start_dir.toFile().listFiles();
        for (File file : files)
            this.items.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));

    }

    // Aggiorna la repository in base a un commit
    public void commit(Commit commit) {
        for (Item modified : commit.getModified().values()) {
            this.items.replace(modified.getName(), modified);
        }
        for (Item added : commit.getAdded().values()) {
            this.items.put(added.getName(), added);
        }
        this.commits.add(commit);
        this.version = this.commits.size();
    }

    // Verifica se un file è stato modificato
    public boolean isModified(File file) throws Exception {
        String checksum = Generator.md5_Of_File(file);
        if (this.items.containsKey(file.getName()))
            if (this.items.get(file.getName()).getChecksum().compareTo(checksum) != 0)
                return true;
        return false;
    }
    public boolean isModified(Item item) throws Exception {
        if (this.items.containsKey(item.getName()))
            if (this.items.get(item.getName()).getChecksum().compareTo(item.getChecksum()) != 0)
                return true;
        return false;
    }

    public boolean add_peer(PeerAddress peer) {
        this.users.add(peer);
        return true;
    }

    public boolean add_Item(Item item) {
        this.items.put(item.getName(), item);
        return true;
    }

    // Getter & Setter
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getVersion() {
        return this.version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public HashSet<PeerAddress> getUsers() {
        return this.users;
    }

    public void setUsers(HashSet<PeerAddress> users) {
        this.users = users;
    }

    public HashMap<String,Item> getItems() {
        return this.items;
    }

    public void setItems(HashMap<String,Item> items) {
        this.items = items;
    }

    public ArrayList<Commit> getCommits() {
        return this.commits;
    }

    public void setCommits(ArrayList<Commit> commits) {
        this.commits = commits;
    }
}