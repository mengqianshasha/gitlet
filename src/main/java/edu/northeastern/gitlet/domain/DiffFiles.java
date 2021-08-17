package edu.northeastern.gitlet.domain;

import java.util.HashMap;

public class DiffFiles {
    private HashMap<String, String> toAddList;
    private HashMap<String, String> toRemoveList;
    private HashMap<String, String> toModifyList;

    public DiffFiles() {
        toAddList = new HashMap<String, String>();
        toRemoveList = new HashMap<String, String>();
        toModifyList = new HashMap<String, String>();
    }

    public HashMap<String, String> getToAddList() {
        return toAddList;
    }

    public HashMap<String, String> getToRemoveList() {
        return toRemoveList;
    }

    public HashMap<String, String> getToModifyList() {
        return toModifyList;
    }

    public void addToAddList(String filePath, String fileHash) {
        this.toAddList.put(filePath, fileHash);
    }

    public void addToRemoveList(String filePath, String fileHash) {
        this.toRemoveList.put(filePath, fileHash);
    }

    public void addToModifyList(String filePath, String fileHash) {
        this.toModifyList.put(filePath, fileHash);
    }
}
