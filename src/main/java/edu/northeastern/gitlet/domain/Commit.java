package edu.northeastern.gitlet.domain;


import java.io.Serializable;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author Qiansha Meng
 */
public class Commit implements Serializable {

    /** The message of this Commit. */
    private String message;
    /** The timestamp of this Commit that indicates the time this Commit is created. */
    private String timestamp;
    /** The parent commit of this Commit. */
    private String parent;
    /** The tree object. */
    private String tree;
    /** The author of the commit */
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
