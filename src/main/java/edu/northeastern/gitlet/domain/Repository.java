package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.exception.GitletException;
import edu.northeastern.gitlet.util.Utils;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.List;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    private static final File CWD = new File(System.getProperty("user.dir"));

    private static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    private static final File OBJECTS_DIR = Utils.join(GITLET_DIR, "objects");

    private static final File REFS_HEADS_DIR = Utils.join(GITLET_DIR, "refs", "heads");

    private static final File HEAD_FILE = Utils.join(GITLET_DIR, "HEAD");

    private static final File INDEX_FILE = Utils.join(GITLET_DIR, "index");

    private static final String HEAD = "HEAD";

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
        Utils.writeContents(HEAD_FILE, "refs/heads/" + this.getConfigValue("init.defaultBranch") + "\n");
        try {
            CONFIG_FILE.createNewFile();
        } catch (IOException e) {
            throw new GitletException("Unexpected error:" + e);
        }
        commit("initial commit");
        return null;
    }

    public void updateConfig(File file, String propertyName, String propertyValue){
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
            properties.setProperty(propertyName, propertyValue);
            properties.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            throw GitletException.error(e.getMessage());
        }
    }

    public String listConfigs(){
        Properties properties = this.loadConfigs();
        StringBuilder sb = new StringBuilder();
        properties.forEach((x, y) -> sb.append(x).append("=").append(y).append("\n"));

        return sb.length() == 0 ? null : sb.toString();
    }

    public String add(String filePath) {
        this.checkRepoExists();
        HashMap<String, String> filesInIndex = this.readIndex();

        File file = Utils.join(CWD, filePath);
        if (file.exists()) {
            filesInIndex.put(filePath, this.hashObject(ObjectType.blob, Utils.readContentsAsString(file)));

        } else if (filesInIndex.containsKey(filePath)){
            filesInIndex.remove(filePath);
        }

        Utils.writeObject(INDEX_FILE, filesInIndex);
        return null;
    }

    public String commit(String comment) {
        Commit commit = null;
        File file = null;

        File defaultBranchFile = Utils.join(REFS_HEADS_DIR, this.getConfigValue("init.defaultBranch"));
        if (!defaultBranchFile.exists()) {

            // init commit
            commit = new Commit(
                    comment,
                    Instant.EPOCH.toString(),
                    null,
                    null,
                    this.getAuthor());
            file = defaultBranchFile;

        } else {
            // commit for staging area
            String parentHash = this.parseReference(HEAD);
            Commit parentCommit = (Commit)this.readObject(parentHash);

            // Step1: compare index and most recent commit
            HashMap<String, String> parentCommitFiles = this.flattenCommitTree(parentCommit);
            HashMap<String, String> indexFiles = this.readIndex();
            DiffFiles diffFiles = new DiffFiles();
            diffFiles.findDiffFiles(indexFiles, parentCommitFiles);

            // Step2: update new commit according to the result of comparing
            String parentCommitTreeHash = parentCommit.getTree();
            HashMap<String, TreeNode> parentCommitTree = (HashMap<String, TreeNode>)this.readObject(parentCommitTreeHash);
            if (!diffFiles.getOrphanFiles1().isEmpty()) {       // file in index, not in commit, then insert
                for (String filePath : diffFiles.getOrphanFiles1()) {
                    String[] path = Utils.splitPath(filePath);
                    HashMap<String, TreeNode> parentCommitCurr = parentCommitTree;
                    for (int i = 0; i < path.length; i++) {
                        if (i < path.length - 1) {

                        }
                    }
                }
            }
            if (!diffFiles.getOrphanFiles2().isEmpty()) {       // file in commit, not in index, then remove

            }
            if (!diffFiles.getModifiedFiles().isEmpty()) {      // file different versions, then update in commit

            }
        }

        String commitHash = this.hashObject(ObjectType.commit, commit);
        Utils.writeContents(file, commitHash + "\n");

        return null;
    }

    public String listFilesFromIndex() {
        this.checkRepoExists();
        StringBuilder sb = new StringBuilder();
        if (INDEX_FILE.exists()) {
            HashMap<String, String> filesInIndex = Utils.readObject(INDEX_FILE, HashMap.class);
            filesInIndex.entrySet().forEach(entry -> {
                sb.append(entry.getValue() + "     " + entry.getKey() + "\n");
            });
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    public String listTree(String hash) {
        this.checkRepoExists();
        Object object = this.readObject(hash);

        if (object instanceof String){
            throw new GitletException("fatal: not a tree object");
        }

        HashMap<String, TreeNode> result = null;
        if (object instanceof Commit){
            result = (HashMap<String, TreeNode>)this.readObject(((Commit)object).getTree());
        }else{
            result = (HashMap<String, TreeNode>)object;
        }

        StringBuilder sb = new StringBuilder();
        result.values().forEach((entry) -> {
            sb.append(entry.getNodeType()).append(" ")
                    .append(entry.getHash()).append("  ").append(entry.getFileName()).append("\n");
        });

        return sb.toString();
    }

    public String parseReference(String name){
        this.checkRepoExists();
        File file = null;
        if (name.equalsIgnoreCase(HEAD)){
            String head = Utils.readContentsAsString(HEAD_FILE).trim();
            file = Utils.join(GITLET_DIR, head);
        }else{
            file = Utils.join(REFS_HEADS_DIR, name);
            if (!file.exists()){
                throw new GitletException("fatal: Not a valid object name" + name);
            }
        }

        return Utils.readContentsAsString(file).trim();
    }

    public String readFile(String hash) {
        this.checkRepoExists();
        Object object = this.readObject(hash);
        if (object instanceof String){
            return (String)object;
        }else if (object instanceof Commit){
            Commit commit = (Commit) object;
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
        } else {
            HashMap<String, TreeNode> files = (HashMap<String, TreeNode>) object;
            StringBuilder sb = new StringBuilder();
            files.values().forEach((entry) -> {
                sb.append(entry.getNodeType()).append(" ")
                        .append(entry.getHash()).append("  ").append(entry.getFileName()).append("\n");
            });
            return sb.toString();
        }
    }

    public String readFileType(String operand) {
        this.checkRepoExists();
        File file = Utils.join(OBJECTS_DIR, operand.substring(0, 2), operand.substring(2));
        if (file.exists()) {
            return Utils.readContentsAsString(file).split("\u0000")[0].split(" ")[0];
        }

        return null;
    }

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

    private HashMap<String, String> flattenCommitTree(Commit commit) {
        String treeHash = commit.getTree();
        HashMap<String, String> result = new HashMap<String, String>();
        if (treeHash == null) {
            return result;
        }
        HashMap<String, TreeNode> files = (HashMap<String, TreeNode>)this.readObject(treeHash);

        Queue<TreeNode> queue = new LinkedList<>();
        for (TreeNode treeNode: files.values()) {
            queue.add(treeNode);
        }

        while (!queue.isEmpty()) {
            TreeNode treeNode = queue.poll();
            if (treeNode.getNodeType() == TreeNodeType.blob) {
                result.put(treeNode.getFileName(), treeNode.getHash());
            } else {
                HashMap<String, TreeNode> subFiles = (HashMap<String, TreeNode>)this.readObject(treeNode.getHash());
                for (TreeNode subTreeNode: subFiles.values()) {
                    subTreeNode.setFileName(Utils.join(treeNode.getFileName(), subTreeNode.getFileName()).getPath());
                    queue.add(subTreeNode);
                }
            }
        }

        return result;
    }

    private Object readObject(String hash) {
        File file = Utils.join(OBJECTS_DIR, hash.substring(0, 2), hash.substring(2));
        if (file.exists()) {
            String[] parts = Utils.readContentsAsString(file).split("\u0000");
            ObjectType objectType = ObjectType.valueOf(parts[0].split(" ")[0]);

            switch (objectType){
                case blob:
                    return parts[1];
                case commit:
                    Commit commit = Utils.deserialize(parts[1], Commit.class);
                    return commit;
                case tree:
                    HashMap<String, TreeNode> tree = Utils.deserialize(parts[1], HashMap.class);
                    return tree;
            }
        }

        return null;
    }

    private HashMap<String, String> readIndex() {
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
        return this.getConfigValue("user.name")  + " <" + this.getConfigValue("user.email") + ">";
    }

    private String getConfigValue(String key){
        Properties properties = this.loadConfigs();
        return properties.getProperty(key);
    }

    private Properties loadConfigs(){
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
