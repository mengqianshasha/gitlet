package edu.northeastern.gitlet.domain;

public class TreeNode {
    private TreeNodeType nodeType;
    private String hash;
    private String fileName;

    public TreeNode(TreeNodeType nodeType, String hash, String fileName){
        this.nodeType = nodeType;
        this.hash = hash;
        this.fileName = fileName;
    }
}
