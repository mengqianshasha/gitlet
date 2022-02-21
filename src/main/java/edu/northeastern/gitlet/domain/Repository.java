package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.exception.GitletException;
import edu.northeastern.gitlet.util.Utils;

import java.io.*;
import java.time.Instant;
import java.util.*;

/***
 * The service class for handling different command
 */
public class Repository {
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

            diffFiles.applyDiff(
                    (filePath) -> this.insertToCommitTree(filePath, indexFiles.get(filePath), parentCommitTree),
                    (filePath) -> this.removeFromCommitTree(filePath, parentCommitTree),
                    (filePath) -> this.updateCommitTree(filePath, indexFiles.get(filePath), parentCommitTree));

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
        String commitHash = this.referenceStore.parseHeadReference();
        StringBuilder sb = new StringBuilder();
        while (commitHash != null) {
            Commit commit = (Commit)this.objectStore.readObject(commitHash);
            sb.append("===\n")
                    .append("commit ")
                    .append(commitHash)
                    .append("\n")
                    .append("Author: ")
                    .append(commit.getAuthor())
                    .append("\n")
                    .append("Date: ")
                    .append(commit.getTimestamp())
                    .append("\n")
                    .append(commit.getMessage())
                    .append("\n\n");
            commitHash = commit.getParent();
        }

        return sb.toString();
    }

    public String branch(String branchName) {
        this.checkRepoExists();
        File branchFile = this.referenceStore.getBranchFile(branchName);
        if (branchFile.exists()) {
            throw new GitletException("A branch with that name already exists.");
        }
        try {
            branchFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Utils.writeContents(branchFile, this.referenceStore.parseHeadReference() + "\n");
        return null;
    }

    public String removeBranch(String branchName) {
        this.checkRepoExists();
        File branchFile = this.referenceStore.getBranchFile(branchName);

        // Error1: the branch does not exist
        if (!branchFile.exists()) {
            throw new GitletException("A branch with that name does not exist.");
        }

        // Error2: the branch for removal is current branch
        if (this.referenceStore.getCurrentBranchName().equals(branchName)) {
            throw new GitletException("Cannot remove the current branch.");
        }

        // Remove
        branchFile.delete();
        return null;
    }

    public String remove(String fileName) {
        this.checkRepoExists();
        File fileInCWD = Utils.join(CWD, fileName);

        HashMap<String, String> indexFiles = this.indexStore.readIndex();
        String commitHash = this.referenceStore.parseHeadReference();
        Commit commit = (Commit)this.objectStore.readObject(commitHash);
        HashMap<String, String> commitFiles = this.flattenCommitTree(commit);

        // Error: the file for removal is neither staged nor tracked by the head commit
        if (!commitFiles.containsKey(fileName)) {
            if (!indexFiles.containsKey(fileName)) {
                throw new GitletException("No reason to remove the file.");
            }
        } else {
            fileInCWD.delete();
        }
        indexFiles.remove(fileName);
        this.indexStore.updateIndex(indexFiles);
        return null;
    }

    public String status() {
        this.checkRepoExists();
        StringBuilder sb = new StringBuilder();

        // 1. All branches
        String currBranch = this.referenceStore.getCurrentBranchName();
        Set<String> branches = this.referenceStore.getAllBranchNames();
        branches.remove(currBranch);
        sb.append("=== Branches ===\n").append("*").append(currBranch).append("\n");
        branches.forEach(branch -> sb.append(branch).append("\n"));
        sb.append("\n");

        // 2. Staged files & Removed files
        HashMap<String, String> indexFiles = this.indexStore.readIndex();
        String currBranchHash = this.referenceStore.parseHeadReference();
        Commit currCommit = (Commit)this.objectStore.readObject(currBranchHash);
        HashMap<String, String> currCommitFiles = this.flattenCommitTree(currCommit);

        DiffFiles diffOfIndexAndCommit = new DiffFiles();
        diffOfIndexAndCommit.findDiffFiles(indexFiles, currCommitFiles);
        Set<String> stagedFiles = new TreeSet<String>();
        Set<String> removedFiles = new TreeSet<String>();
        diffOfIndexAndCommit.applyDiff(fileName -> stagedFiles.add(fileName),
                fileName -> removedFiles.add(fileName),
                fileName -> stagedFiles.add(fileName));
        sb.append("=== Staged Files ===\n");
        stagedFiles.forEach(fileName -> sb.append(fileName).append("\n"));
        sb.append("\n");
        sb.append("=== Removed Files ===\n");
        removedFiles.forEach(fileName -> sb.append(fileName).append("\n"));
        sb.append("\n");

        // Unstaged & Untracked
        List<String> CWDFileNames = Utils.plainFilenamesIn(CWD);
        HashMap<String, String> CWDFiles = new HashMap<>();
        for (String fileName : CWDFileNames) {
            File file = Utils.join(CWD, fileName);
            String fileHash = this.objectStore.hashObject(ObjectType.blob,
                    Utils.readContentsAsString(file),
                    false);
            CWDFiles.put(fileName, fileHash);
        }
        DiffFiles diffOfCWDAndIndex = new DiffFiles();
        diffOfCWDAndIndex.findDiffFiles(CWDFiles, indexFiles);
        TreeSet<String> untracked = new TreeSet<>();
        TreeSet<String> deleted = new TreeSet<>();
        TreeSet<String> modified = new TreeSet<>();
        diffOfCWDAndIndex.applyDiff(fileName -> untracked.add(fileName),
                fileName -> deleted.add(fileName),
                fileName -> modified.add(fileName));
        sb.append("=== Modifications Not Staged For Commit ===\n");
        deleted.forEach(fileName -> sb.append(fileName).append(" (deleted)\n"));
        modified.forEach(fileName -> sb.append(fileName).append(" (modified)\n"));
        sb.append("\n");
        sb.append("=== Untracked Files ===\n");
        untracked.forEach(fileName -> sb.append(fileName).append("\n"));
        sb.append("\n");
        return sb.toString();
    }

    public String checkoutFile(String fileName) {
        this.checkRepoExists();
        String commitHash = this.referenceStore.parseHeadReference();
        return this.checkoutFileFromCommit(commitHash, fileName);
    }

    public String checkoutFileFromCommit(String commitHash, String fileName) {
        this.checkRepoExists();

        // Error1: the commit does not exist
        Commit commit = (Commit)this.objectStore.readObject(commitHash);
        if (commit == null) {
            throw new GitletException("No commit with that id exists.");
        }

        // Error2: the file does not exist in the commit
        HashMap<String, String> commitFiles = this.flattenCommitTree(commit);
        if (!commitFiles.containsKey(fileName)) {
            throw new GitletException("File does not exist in that commit.");
        }

        // Put the file in the head commit in the working directory
        // Or overwrite the existing file in the working directory
        File file = Utils.join(CWD, fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String contentsInFile = (String)this.objectStore.readObject(commitFiles.get(fileName));
        Utils.writeContents(file, contentsInFile);
        return null;
    }

    public String checkoutBranch(String branchName) {
        this.checkRepoExists();

        // Error1: no need to checkout current branch
        String currBranchName = this.referenceStore.getCurrentBranchName();
        if (branchName.equals(currBranchName)) {
            throw new GitletException("No need to checkout the current branch");
        }

        // Error2: no such branch
        this.validateBranchName(branchName);

        // Error3: untracked file in the working directory
        this.checkUntrackedFilesWithBranchName(branchName);

        // Overwrite CWD with checked out branch
        this.overwriteCWD(branchName);

        // Clear the stage
        HashMap<String, String> branchFiles = this.flattenBranchTree(branchName);
        this.indexStore.updateIndex(branchFiles);

        // Switch the current branch to the checked out branch
        this.referenceStore.setHead(branchName);
        return null;
    }

    public String reset(String commitHash) {
        this.checkRepoExists();
        this.validateCommitHash(commitHash);
        this.checkUntrackedFilesWithCommitHash(commitHash);
        this.overwriteCWDWithCommitHash(commitHash);

        // Clear the stage
        HashMap<String, String> commitFiles = this.flattenCommitTree(commitHash);
        this.indexStore.updateIndex(commitFiles);

        // Move the current branch's head to that commit
        File currBranchFile = this.referenceStore.getBranchFile(this.referenceStore.getCurrentBranchName());
        Utils.writeContents(currBranchFile, commitHash);

        return null;
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

            sb.append("author ")
                    .append(commit.getAuthor())
                    .append("\n\n")
                    .append(commit.getMessage())
                    .append("\n");
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

    private void checkUntrackedFilesWithCommitHash(String commitHash) {
        Commit commit = (Commit)this.objectStore.readObject(commitHash);
        this.checkUntrackedFiles(commit);
    }
    private void validateCommitHash(String commitHash) {
        Commit commit = (Commit)this.objectStore.readObject(commitHash);
        if (commit == null) {
            throw new GitletException("No commit with that id exists.");
        }
    }

    private void validateBranchName(String branchName) {
        Set<String> allBranchNames = this.referenceStore.getAllBranchNames();
        if (!allBranchNames.contains(branchName)) {
            throw new GitletException("No such branch exists");
        }
    }

    private void checkUntrackedFiles(Commit commit) {
        List<String> CWDFileNames = Utils.plainFilenamesIn(CWD);
        HashMap<String, String> currCommitFiles = this.flattenBranchTree(this.referenceStore.getCurrentBranchName());
        HashMap<String, String> targetCommitFiles = this.flattenCommitTree(commit);

        for (String fileName : CWDFileNames) {
            File file = Utils.join(CWD, fileName);
            String fileHash = this.objectStore.hashObject(ObjectType.blob,
                    Utils.readContentsAsString(file),
                    false);
            if (!currCommitFiles.containsKey(fileName) &&
                    (!targetCommitFiles.containsKey(fileName) ||
                            !fileHash.equals(targetCommitFiles.get(fileName)))) {
                throw new GitletException("There is an untracked file in the way; " +
                        "delete it, or add and commit it first.");
            }
        }
    }

    private void checkUntrackedFilesWithBranchName(String branchName) {
        String branchCommitHash = this.referenceStore.parseReference(branchName);
        Commit commit = (Commit)this.objectStore.readObject(branchCommitHash);
        this.checkUntrackedFiles(commit);
    }

    private void overwriteCWDWithCommitHash(String commitHash) {
        List<String> CWDFileNames = Utils.plainFilenamesIn(CWD);
        HashMap<String, String> targetCommitFiles = this.flattenCommitTree(commitHash);

        for (String fileName : CWDFileNames) {
            File file = Utils.join(CWD, fileName);
            if (!targetCommitFiles.containsKey(fileName)) {
                file.delete();
            }
        }
        for (String fileName : targetCommitFiles.keySet()) {
            File file = Utils.join(CWD, fileName);
            if (!CWDFileNames.contains(fileName)) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Utils.writeContents(file, (String)this.objectStore.readObject(targetCommitFiles.get(fileName)));
        }
    }

    private void overwriteCWD(String branchName) {
        String branchCommitHash = this.referenceStore.parseReference(branchName);
        this.overwriteCWDWithCommitHash(branchCommitHash);
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

    private HashMap<String, String> flattenCommitTree(String commitHash) {
        Commit commit = (Commit)this.objectStore.readObject(commitHash);
        if (commit == null) {
            throw new GitletException("No commit with that id exists.");
        }
        return this.flattenCommitTree(commit);
    }

    private HashMap<String, String> flattenBranchTree(String branchName) {
        String branchCommitHash = this.referenceStore.parseReference(branchName);
        return this.flattenCommitTree(branchCommitHash);
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
