package edu.northeastern.gitlet.domain;

import java.io.Serializable;


public class Commit implements Serializable {

    private String message;
    private String timestamp;
    private String parent;
    private String tree;
    private String author;

    public Commit(String message, String timestamp, String parent, String tree, String author) {
        this.message = message;
        this.timestamp = timestamp;
        this.parent = parent;
        this.tree = tree;
        this.author = author;
    }

    public String getMessage() {
        return message;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getParent() {
        return parent;
    }

    public String getTree() {
        return tree;
    }

    public String getAuthor() {
        return author;
    }
}
