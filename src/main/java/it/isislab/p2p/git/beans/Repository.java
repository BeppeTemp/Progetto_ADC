package it.isislab.p2p.git.beans;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;

import net.tomp2p.peers.PeerAddress;

public class Repository implements Serializable {
    private String name;
    private HashSet<PeerAddress> users;
    private ArrayList<Item> items;
    private ArrayList<Commit> commits;

    private static Md5_gen gen = new Md5_gen();

    // Costruttore
    public Repository(String name, HashSet<PeerAddress> users, File directory) throws Exception {
        this.name = name;
        this.users = users;

        this.items = new ArrayList<Item>();
        this.commits = new ArrayList<Commit>();

        File[] files = directory.listFiles();
        for (File file : files)
            this.items.add(new Item(file.getName(), gen.md5_Of_File(file), Files.readAllBytes(file.toPath())));

    }
    
    // Aggiorna la repository in base a un commit
    public void commit(Commit commit) {
        this.commits.add(commit);
        for (Item modified : commit.getModified()) {
            for (int i = 0; i < this.items.size(); i++) {
                if (this.items.get(i).getName().compareTo(modified.getName()) == 0) {
                    this.items.get(i).setBytes(modified.getBytes());
                    this.items.get(i).setChecksum(modified.getChecksum());
                }
            }
        }
        for (Item added : commit.getAdded()) {
            this.items.add(added);
        }
    }

    public boolean isDifferent(File file) throws Exception {
        int i = this.contains(file);
        System.out.println("Indice: " + i);
        System.out.println("File: " + this.items.get(i).getName());
        if (i != -1) {
            if (gen.md5_Of_File(file).compareTo(this.items.get(i).getChecksum()) != 0) {
                return true;
            }
        }
        return false;
    }
    
    // Verifica se un file è già contenuto nella repository e ne ritorna la posizione
    public int contains(File file) throws Exception {
        String checksum = gen.md5_Of_File(file);
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).getChecksum().compareTo(checksum) == 0 && this.items.get(i).getName().compareTo(file.getName()) == 0) {
                return i;
            }
        }
        return -1;
    }
    
    // Verifica se un file è stato modificato
    public boolean isModified(File file) throws Exception {
        String checksum = gen.md5_Of_File(file);
        for (Item item : this.items) {
            if (file.getName().compareTo(item.getName()) == 0 && item.getChecksum().compareTo(checksum) != 0) {
                return true;
            }
        }
        return false;
    }

    // Aggiunge un item alla repository
    public void add_Item(Item item) {
        this.items.add(item);
    }
    
    // Getter & Setter
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashSet<PeerAddress> getUsers() {
        return this.users;
    }

    public void setUsers(HashSet<PeerAddress> users) {
        this.users = users;
    }

    public ArrayList<Item> getItems() {
        return this.items;
    }

    public void setItems(ArrayList<Item> items) {
        this.items = items;
    }

    public ArrayList<Commit> getCommits() {
        return this.commits;
    }

    public void setCommits(ArrayList<Commit> commits) {
        this.commits = commits;
    }

}