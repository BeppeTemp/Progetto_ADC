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

    // Costruttori
    public Repository(String name, HashSet<PeerAddress> users, File directory) throws Exception {
        this.name = name;
        this.users = users;

        this.items = new ArrayList<Item>();
        this.commits = new ArrayList<Commit>();

        File[] files = directory.listFiles();
        for (File file : files)
            this.items.add(new Item(file.getName(), gen.md5_Of_File(file), Files.readAllBytes(file.toPath())));

    }

    // Aggiunge file alla repository
    public void add_Files(File[] files) throws Exception {
        Integer i;

        for (File file : files) {
            i = contains(file);
            System.out.println(file.getName());
            System.out.println(i);
            if (i == -1) {
                System.out.println("Aggiunto: " + file.getName());
                this.items.add(new Item(file.getName(), gen.md5_Of_File(file), Files.readAllBytes(file.toPath())));
            }
        }
    }

    // Verifica se un file è già contenuto nella repository
    public Integer contains(File file) throws Exception {
        String checksum = gen.md5_Of_File(file);
        for (Integer i = 0; i < this.items.size(); i++) {
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
}