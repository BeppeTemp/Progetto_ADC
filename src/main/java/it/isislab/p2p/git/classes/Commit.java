package it.isislab.p2p.git.classes;

import java.io.Serializable;
import java.util.HashMap;

public class Commit implements Serializable{
    private String message;
    private HashMap<String, Item> modified;
    private HashMap<String, Item> added;

    // Costruttore
    public Commit(String message, HashMap<String, Item> modified, HashMap<String, Item> added) {
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

    public HashMap<String,Item> getModified() {
        return this.modified;
    }

    public void setModified(HashMap<String,Item> modified) {
        this.modified = modified;
    }

    public HashMap<String,Item> getAdded() {
        return this.added;
    }

    public void setAdded(HashMap<String,Item> added) {
        this.added = added;
    }

}