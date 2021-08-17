package edu.northeastern.gitlet.domain;

import java.io.Serializable;

public class TreeNode implements Serializable {
    private TreeNodeType nodeType;
    private String hash;
    private String fileName;

    public TreeNode(TreeNodeType nodeType, String hash, String fileName){
        this.nodeType = nodeType;
        this.hash = hash;
        this.fileName = fileName;
    }
}
