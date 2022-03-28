package it.isislab.p2p.git.beans;

import java.util.ArrayList;

public class Commit {
    private String message;
    private ArrayList<Item> modified;

    // Costruttore
    public Commit(String message, ArrayList<Item> modified) {
        this.message = message;
        this.modified = modified;
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
}