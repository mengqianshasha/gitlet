package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.exception.GitletException;
import edu.northeastern.gitlet.util.Utils;

import java.io.File;

public class ReferenceStore {
    private static final File REFS_HEADS_DIR = Utils.join(Repository.GITLET_DIR, "refs", "heads");

    private static final File HEAD_FILE = Utils.join(Repository.GITLET_DIR, "HEAD");

    private static final String HEAD = "HEAD";

    public void initReferenceStore(String defaultBranchName){
        REFS_HEADS_DIR.mkdirs();
        Utils.writeContents(HEAD_FILE,
                "refs" + File.separator + "heads" + File.separator + defaultBranchName + "\n");
    }

    public String parseHeadReference() {
        return this.parseReference(HEAD);
    }

    public File getCurrentBranchFile() {
        String head = Utils.readContentsAsString(HEAD_FILE).trim();
        return Utils.join(Repository.GITLET_DIR, head);
    }

    public String getCurrentBranchName() {
        String head = Utils.readContentsAsString(HEAD_FILE).trim();
        String[] path = Utils.splitPath(head);
        return path[path.length - 1];
    }

    public File getBranchFile(String branchName){
        return Utils.join(REFS_HEADS_DIR, branchName);
    }

    public String parseReference(String name){
        File file = null;
        if (name.equalsIgnoreCase(HEAD)){
            String head = Utils.readContentsAsString(HEAD_FILE).trim();
            file = Utils.join(Repository.GITLET_DIR, head);
        }else{
            file = Utils.join(REFS_HEADS_DIR, name);
            if (!file.exists()){
                throw new GitletException("fatal: Not a valid object name " + name);
            }
        }

        return Utils.readContentsAsString(file).trim();
    }

}
