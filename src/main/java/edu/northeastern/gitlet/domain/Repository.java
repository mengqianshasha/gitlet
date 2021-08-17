package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.exception.GitletException;
import edu.northeastern.gitlet.util.Utils;

import java.io.*;
import java.time.Instant;
import java.util.*;

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
    public static final File GITLET_DIR = Utils.join(CWD, ".gitlet");

    private ConfigStore configStore;
    private ObjectStore objectStore;
    private IndexStore indexStore;
    private ReferenceStore referenceStore;

    public Repository(){
        this.configStore = new ConfigStore();
        this.objectStore = new ObjectStore();
        this.indexStore = new IndexStore();
        this.referenceStore = new ReferenceStore();
    }

    public String initRepo() {
        if (this.exists()) {
            throw new GitletException("A Gitlet version-control system already exists in the current directory.");
        }
        GITLET_DIR.mkdir();
        this.objectStore.initObjectStore();
        this.referenceStore.initReferenceStore(this.configStore.getConfigValue("init.defaultBranch"));
        this.configStore.initConfigStore();
        commit("initial commit");
        return null;
    }

    public void updateConfig(File file, String propertyName, String propertyValue){
        this.configStore.updateConfig(file, propertyName, propertyValue);
    }

    public String listConfigs(){
        Properties properties = this.configStore.loadConfigs();
        StringBuilder sb = new StringBuilder();
        properties.forEach((x, y) -> sb.append(x).append("=").append(y).append("\n"));

        return sb.length() == 0 ? null : sb.toString();
    }

    public String add(String filePath) {
        this.checkRepoExists();
        HashMap<String, String> filesInIndex = this.indexStore.readIndex();

        File file = Utils.join(CWD, filePath);
        if (file.exists()) {
            filesInIndex.put(filePath, this.objectStore.hashObject(ObjectType.blob, Utils.readContentsAsString(file)));

        } else if (filesInIndex.containsKey(filePath)){
            filesInIndex.remove(filePath);
        }

        this.indexStore.updateIndex(filesInIndex);
        return null;
    }

    public String commit(String comment) {
        Commit commit = null;
        File file = null;

        File defaultBranchFile = this.referenceStore
                .getBranchFile(this.configStore.getConfigValue("init.defaultBranch"));
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
            String parentHash = this.referenceStore.parseHeadReference();
            Commit parentCommit = (Commit)this.objectStore.readObject(parentHash);

            // Step1: compare index and most recent commit
            HashMap<String, String> parentCommitFiles = this.flattenCommitTree(parentCommit);
            HashMap<String, String> indexFiles = this.indexStore.readIndex();
            DiffFiles diffFiles = new DiffFiles();
            diffFiles.findDiffFiles(indexFiles, parentCommitFiles);

            // Step2: update new commit according to the result of comparing
            HashMap<String, TreeNode> parentCommitTree = this.getCommitTreeFromCommit(parentCommit);

            if (!diffFiles.getOrphanFiles1().isEmpty()) {       // file in index, not in commit, then insert
                for (String filePath : diffFiles.getOrphanFiles1()) {
                    this.insertToCommitTree(filePath, indexFiles.get(filePath), parentCommitTree);
                }
            }
            if (!diffFiles.getOrphanFiles2().isEmpty()) {       // file in commit, not in index, then remove
                for (String filePath : diffFiles.getOrphanFiles2()) {
                    this.removeFromCommitTree(filePath, parentCommitTree);
                }
            }
            if (!diffFiles.getModifiedFiles().isEmpty()) {      // file different versions, then update in commit
                for (String filePath : diffFiles.getModifiedFiles()) {
                    this.updateCommitTree(filePath, indexFiles.get(filePath), parentCommitTree);
                }
            }

            // Step3: Construct new commit
            String treeHash = this.hashObject(ObjectType.tree, parentCommitTree);
            commit = new Commit(comment, Instant.now().toString(), parentHash, treeHash, this.getAuthor());

            file = this.referenceStore.getCurrentBranchFile();
        }

        String commitHash = this.objectStore.hashObject(ObjectType.commit, commit);
        Utils.writeContents(file, commitHash + "\n");

        return null;
    }

    public String log() {
        this.checkRepoExists();
        String commitHash = this.parseReference("HEAD");
        StringBuilder sb = new StringBuilder();
        while (commitHash != null) {
            Commit commit = (Commit)this.readObject(commitHash);
            sb.append("===\n");
            sb.append("commit ");
            sb.append(commitHash + "\n");
            sb.append("Date: " + commit.getTimestamp() + "\n");
            sb.append(commit.getMessage() + "\n\n");
            commitHash = commit.getParent();
        }
        return sb.toString();
    }

    public String branch(String branchName) {
        this.checkRepoExists();
        File branchFile = Utils.join(REFS_HEADS_DIR, branchName);
        if (branchFile.exists()) {
            throw new GitletException("A branch with that name already exists.");
        }
        try {
            branchFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(branchFile, this.parseReference("HEAD") + "\n");
        return null;
    }

    private void removeFromCommitTree(String filePath, HashMap<String, TreeNode> commitTree) {
        commitTree.remove(filePath);
    }

    private void insertToCommitTree(String filePath, String fileHash, HashMap<String, TreeNode> commitTree) {
        TreeNode treeNode = new TreeNode(TreeNodeType.blob, fileHash, filePath);
        commitTree.put(filePath, treeNode);
    }

    private void updateCommitTree(String filePath, String fileHash, HashMap<String, TreeNode> commitTree) {
        TreeNode treeNode = new TreeNode(TreeNodeType.blob, fileHash, filePath);
        commitTree.put(filePath, treeNode);
    }
    public String listFilesFromIndex() {
        this.checkRepoExists();
        StringBuilder sb = new StringBuilder();
        HashMap<String, String> filesInIndex = this.indexStore.readIndex();
        filesInIndex.entrySet().forEach(entry -> {
            sb.append(entry.getValue() + "     " + entry.getKey() + "\n");
        });

        return sb.length() == 0 ? null : sb.toString();
    }

    public String listTree(String hash) {
        this.checkRepoExists();
        Object object = this.readObjectWithReferenceParsing(hash);

        if (object instanceof String){
            throw new GitletException("fatal: not a tree object");
        }

        HashMap<String, TreeNode> result = null;
        if (object instanceof Commit){
            String tree = ((Commit)object).getTree();
            if (tree == null){
                throw new GitletException("fatal: not a tree object");
            }

            result = (HashMap<String, TreeNode>)this.readObjectWithReferenceParsing(tree);
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
        return this.referenceStore.parseReference(name);
    }

    public String readFile(String hash) {
        this.checkRepoExists();
        Object object = this.readObjectWithReferenceParsing(hash);
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

    public String readFileType(String hash) {
        this.checkRepoExists();
        return this.readFileTypeWithReferenceParsing(hash);
    }

    public String hashObject(ObjectType objectType, Object o) {
        this.checkRepoExists();
        return this.objectStore.hashObject(objectType, o);
    }

    private String readFileTypeWithReferenceParsing(String hash){
        String fileType = this.objectStore.readFileType(hash);
        if (fileType == null){
            fileType = this.objectStore.readFileType(this.referenceStore.parseReference(hash));
        }

        return fileType;
    }

    private Object readObjectWithReferenceParsing(String hash) {
        Object object = this.objectStore.readObject(hash);
        if (object == null){
            object = this.objectStore.readObject(this.referenceStore.parseReference(hash));
        }

        return object;
    }

    private HashMap<String, TreeNode> getCommitTreeFromCommit(Commit commit){
        String treeHash = commit.getTree();
        if (treeHash == null) {
            return new HashMap<String, TreeNode>();
        }

        return (HashMap<String, TreeNode>)this.objectStore.readObject(treeHash);
    }

    private HashMap<String, String> flattenCommitTree(Commit commit) {
        String treeHash = commit.getTree();
        HashMap<String, String> result = new HashMap<String, String>();
        if (treeHash == null) {
            return result;
        }

        HashMap<String, TreeNode> files = (HashMap<String, TreeNode>)this.objectStore.readObject(treeHash);

        Queue<TreeNode> queue = new LinkedList<>();
        for (TreeNode treeNode: files.values()) {
            queue.add(treeNode);
        }

        while (!queue.isEmpty()) {
            TreeNode treeNode = queue.poll();
            if (treeNode.getNodeType() == TreeNodeType.blob) {
                result.put(treeNode.getFileName(), treeNode.getHash());
            } else {
                HashMap<String, TreeNode> subFiles = (HashMap<String, TreeNode>)this.objectStore.readObject(treeNode.getHash());
                for (TreeNode subTreeNode: subFiles.values()) {
                    subTreeNode.setFileName(Utils.join(treeNode.getFileName(), subTreeNode.getFileName()).getPath());
                    queue.add(subTreeNode);
                }
            }
        }

        return result;
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
        return this.configStore.getConfigValue("user.name")  + " <" + this.configStore.getConfigValue("user.email") + ">";
    }
}
