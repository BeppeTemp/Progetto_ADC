package it.isislab.p2p.git.beans;

import java.io.Serializable;

public class Item implements Serializable{
    private String name;
    private String checksum;
    private byte[] bytes;

    // Costruttore
    public Item(String item_name, String checksum, byte[] file_bytes) {
        this.name = item_name;
        this.bytes = file_bytes;
        this.checksum = checksum;
    }

    // Getter & Setter
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChecksum() {
        return this.checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public void setBytes(byte[] bytes) {
        this.bytes = bytes;
    }
}