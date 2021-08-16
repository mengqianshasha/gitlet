package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.exception.GitletException;
import edu.northeastern.gitlet.util.Utils;

import java.io.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    private static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    private static final File GITLET_DIR = Utils.join(CWD, ".gitlet");
    /** The objects directory. */
    private static final File OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");
    /** The refs directory. */
    private static final File REFS_HEADS_DIR = Utils.join(GITLET_DIR, "refs", "heads");
    /** The HEAD file. */
    private static final File HEAD_FILE = Utils.join(GITLET_DIR, "HEAD");
    /** The index file. */
    private static final File INDEX_FILE = Utils.join(GITLET_DIR, "index");
    /** The config file */
    public static final File CONFIG_FILE = Utils.join(GITLET_DIR, "config");

    public static final File GLOBAL_CONFIG_FILE = Utils.join(
            new File(System.getProperty("user.home")), ".gitletconfig");

    public String initRepo() {
        if (this.exists()) {
            throw new GitletException("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        REFS_HEADS_DIR.mkdirs();
        Utils.writeContents(HEAD_FILE, "ref: refs/heads/" + this.getProperty("init.defaultBranch") + "\n");
        try {
            CONFIG_FILE.createNewFile();
        } catch (IOException e) {
            throw new GitletException("Unexpected error:" + e);
        }
        commit("initial commit");
        return null;
    }

    public String commit(String comment) {
        Commit commit = null;
        File file = null;

        File defaultBranchFile = Utils.join(REFS_HEADS_DIR, this.getProperty("init.defaultBranch"));
        if (!defaultBranchFile.exists()) {
            commit = new Commit(
                    comment,
                    Instant.EPOCH.toString(),
                    null,
                    null,
                    this.getAuthor());
            file = defaultBranchFile;
        } else {
            String head = Utils.readContentsAsString(HEAD_FILE).split(": ")[1].trim();
            file = Utils.join(GITLET_DIR, head);
        }

        String commitHash = this.hashObject(ObjectType.commit, commit);
        Utils.writeContents(file, commitHash + "\n");

        return null;
    }

    public String config(File file, String propertyName, String propertyValue){
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
            properties.setProperty(propertyName, propertyValue);
            properties.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            throw GitletException.error(e.getMessage());
        }

        return null;
    }

    public String listConfig(){
        Properties properties = this.getProperties();
        StringBuilder sb = new StringBuilder();
        properties.forEach((x, y) -> sb.append(x).append("=").append(y).append("\n"));

        return sb.length() == 0 ? null : sb.toString();
    }

    public String add(String fileName) {
        this.checkRepoExists();
        HashMap<String, String> filesInIndex = this.getHashMapFromIndex();

        File file = Utils.join(CWD, fileName);
        if (file.exists()) {
            filesInIndex.put(fileName, this.hashObject(ObjectType.blob, Utils.readContentsAsString(file)));

        } else if (filesInIndex.containsKey(fileName)){
            filesInIndex.remove(fileName);
        }

        Utils.writeObject(INDEX_FILE, filesInIndex);
        return null;
    }

    public String listFilesFromCWD(){
        this.checkRepoExists();
        StringBuilder sb = new StringBuilder();
        List<String> files = Utils.plainFilenamesIn(CWD);
        files.forEach((entry) -> sb.append(entry).append("\n"));
        return sb.toString();
    }

    public String listFilesFromIndex() {
        this.checkRepoExists();
        StringBuilder sb = new StringBuilder();
        if (INDEX_FILE.exists()) {
            HashMap<File, String> filesInIndex = Utils.readObject(INDEX_FILE, HashMap.class);
            filesInIndex.entrySet().forEach(entry -> {
                sb.append(entry.getValue() + "     " + entry.getKey() + "\n");
            });
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public String readFile(String operand) {
        this.checkRepoExists();
        File file = Utils.join(OBJECTS_DIR, operand.substring(0, 2), operand.substring(2));
        if (file.exists()) {
            String[] parts = Utils.readContentsAsString(file).split("\u0000");
            ObjectType objectType = ObjectType.valueOf(parts[0].split(" ")[0]);

            switch (objectType){
                case blob:
                    return parts[1];
                case commit:
                    Commit commit = Utils.deserialize(parts[1], Commit.class);
                    StringBuilder sb = new StringBuilder();
                    if (commit.getTree() != null){
                        sb.append("tree " + commit.getTree() + "\n");
                    }

                    if (commit.getParent() != null){
                        sb.append("parent " + commit.getParent() + "\n");
                    }

                    sb.append("author " + commit.getAuthor() + "\n");
                    sb.append("\n");
                    sb.append(commit.getMessage() + "\n");

                    return sb.toString();
                case tree:
                    return null;
            }
        }

        return null;
    }

    public String readFileType(String operand) {
        this.checkRepoExists();
        File file = Utils.join(OBJECTS_DIR, operand.substring(0, 2), operand.substring(2));
        if (file.exists()) {
            return Utils.readContentsAsString(file).split("\u0000")[0].split(" ")[0];
        }

        return null;
    }

    /**
     * Compute object ID and optionally creates a gitlet object
     *
     * @param o The given object
     * @return Computed hash
     */
    public String hashObject(ObjectType objectType, Object o) {
        if (!(o instanceof String) && !(o instanceof byte[]) && (o instanceof Serializable)) {
            o = Utils.serialize((Serializable) o);
        }

        String objectHash = Utils.sha1(
                objectType.name(),
                " ",
                Integer.valueOf(Utils.getContentLength(o)).toString(),
                "\u0000",
                o);
        File filePath = Utils.join(OBJECTS_DIR, objectHash.substring(0, 2), objectHash.substring(2));
        if (!filePath.exists()) {
            Utils.writeContents(
                    filePath,
                    objectType.name(),
                    " ",
                    Integer.valueOf(Utils.getContentLength(o)).toString(),
                    "\u0000",
                    o);
        }

        return objectHash;
    }

    private HashMap<String, String> getHashMapFromIndex() {
        if (!INDEX_FILE.exists()) {
            return new HashMap<String, String>();
        }
        return Utils.readObject(INDEX_FILE, HashMap.class);
    }

    private boolean exists() {
        return GITLET_DIR.exists();
    }

    private void checkRepoExists() {
        if (!exists()) {
            throw new GitletException("Not in an initialized Gitlet directory.");
        }
    }

    private String getAuthor(){
        return this.getProperty("user.name")  + " <" + this.getProperty("user.email") + ">";
    }

    private String getProperty(String key){
        Properties properties = this.getProperties();
        return properties.getProperty(key);
    }

    private Properties getProperties(){
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(ConfigScope.Global.getConfigLocation()));
        } catch (IOException e) {
            throw GitletException.error(e.getMessage());
        }

        try {
            properties.load(new FileInputStream(ConfigScope.Repo.getConfigLocation()));
        } catch (IOException e) {
            //ignore exception if repo specific config file doesn't exist yet
        }

        return properties;
    }
}
