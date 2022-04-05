package it.isislab.p2p.git.classes;

import java.io.Serializable;
import java.util.ArrayList;

public class Commit implements Serializable{
    private String message;
    private ArrayList<Item> modified;
    private ArrayList<Item> added;

    // Costruttore
    public Commit(String message, ArrayList<Item> modified, ArrayList<Item> added) {
        this.message = message;
        this.modified = modified;
        this.added = added;
    }

    // Getter & Setter
    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ArrayList<Item> getModified() {
        return this.modified;
    }

    public void setModified(ArrayList<Item> modified) {
        this.modified = modified;
    }

    public ArrayList<Item> getAdded() {
        return this.added;
    }

    public void setAdded(ArrayList<Item> added) {
        this.added = added;
    }

}