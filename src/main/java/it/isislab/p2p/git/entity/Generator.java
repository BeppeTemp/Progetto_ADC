package it.isislab.p2p.git.entity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

public class Generator {
    public static String md5_Of_File(File file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            FileInputStream fs = new FileInputStream(file);
            BufferedInputStream bs = new BufferedInputStream(fs);
            byte[] buffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = bs.read(buffer, 0, buffer.length)) != -1) {
                md.update(buffer, 0, bytesRead);
            }
            byte[] digest = md.digest();

            StringBuilder sb = new StringBuilder();
            for (byte bite : digest) {
                sb.append(String.format("%02x", bite & 0xff));
            }

            bs.close();
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}