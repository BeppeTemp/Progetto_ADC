package it.isislab.p2p.git.entity;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import net.tomp2p.peers.PeerAddress;

public class Repository implements Serializable {
    private String name;
    private int owner;
    private int version;
    private HashSet<PeerAddress> users;
    private HashMap<String, Item> items;
    private ArrayList<Commit> commits;

    // Costruttore
    public Repository(String name, int owner,HashSet<PeerAddress> users, Path start_dir) {
        this.name = name;
        this.users = users;
        this.owner = owner;

        this.items = new HashMap<String, Item>();
        this.commits = new ArrayList<Commit>();

        this.version = this.commits.size();

        File[] files = start_dir.toFile().listFiles();
        if (files != null) {
            for (File file : files)
                try {
                    this.items.put(file.getName(), new Item(file.getName(), Generator.md5_Of_File(file), Files.readAllBytes(file.toPath())));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
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
    public boolean isModified(File file) {
        String checksum = Generator.md5_Of_File(file);
        if (this.items.containsKey(file.getName()))
            if (this.items.get(file.getName()).getChecksum().compareTo(checksum) != 0)
                return true;
        return false;
    }

    public boolean isModified(Item item) {
        if (this.items.containsKey(item.getName()))
            if (this.items.get(item.getName()).getChecksum().compareTo(item.getChecksum()) != 0)
                return true;
        return false;
    }

    // Verifica se il file è contenuto
    public boolean contains(File file) {
        String checksum = Generator.md5_Of_File(file);
        if (this.items.containsKey(file.getName()))
            if (this.items.get(file.getName()).getChecksum().compareTo(checksum) == 0)
                return true;
        return false;
    }

    public boolean add_peer(PeerAddress peer) {
        this.users.add(peer);
        return true;
    }

    public boolean remove_peer(PeerAddress peer) {
        this.users.remove(peer);
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

    public int getOwner() {
        return this.owner;
    }

    public void setOwner(int owner) {
        this.owner = owner;
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


    @Override
    public String toString() {
        String repo = "\n--------------------------------------------------------------------------------" + "\n🔹 Name: " + this.getName() + "\n🔹 Version: " + this.getVersion()
                + "\n🔹 Owner ID: " + this.getOwner() + "\n--------------------------------------------------------------------------------" + "\nFile contenuti:\n";

        for (Item item : this.getItems().values()) {
            repo += "\t🔸 " + item.getName() + " - " + item.getChecksum() + " - " + item.getBytes().length + " bytes\n";
        }
        repo += "--------------------------------------------------------------------------------";

        return repo;
    }

}