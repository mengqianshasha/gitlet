package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.util.Utils;

import java.io.File;
import java.util.HashMap;

public class IndexStore {
    private static final File INDEX_FILE = Utils.join(Repository.GITLET_DIR, "index");

    public HashMap<String, String> readIndex() {
        if (!INDEX_FILE.exists()) {
            return new HashMap<String, String>();
        }
        return Utils.readObject(INDEX_FILE, HashMap.class);
    }

    public void updateIndex(HashMap<String, String> index){
        Utils.writeObject(INDEX_FILE, index);
    }
}
