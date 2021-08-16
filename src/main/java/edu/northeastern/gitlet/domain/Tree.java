package edu.northeastern.gitlet.domain;

import java.util.ArrayList;
import java.util.List;

public class Tree {
    private List<TreeNode> treeNodes;

    public Tree(){
        this.treeNodes = new ArrayList<>();
    }

    public List<TreeNode> GetTreeNodes(){
        return this.treeNodes;
    }

    public void AddTreeNode(TreeNode node){
        this.treeNodes.add(node);
    }
}
