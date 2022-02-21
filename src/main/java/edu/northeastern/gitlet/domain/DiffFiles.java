package edu.northeastern.gitlet.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Consumer;

public class DiffFiles {
    private HashSet<String> orphanFiles1;
    private HashSet<String> orphanFiles2;
    private HashSet<String> modifiedFiles;

    public DiffFiles() {
        orphanFiles1 = new HashSet<String>();
        orphanFiles2 = new HashSet<String>();
        modifiedFiles = new HashSet<String>();
    }

    public void findDiffFiles(HashMap<String, String> area1, HashMap<String, String> area2) {
        for (String filePath : area1.keySet()) {
            // file in both area1 and area2
            if (area2.containsKey(filePath)) {
                if (!area1.get(filePath).equals(area2.get(filePath))) {
                    this.modifiedFiles.add(filePath);
                }
            }
            // file only in area1
            else {
                this.orphanFiles1.add(filePath);
            }
        }
        for (String filePath : area2.keySet()) {
            // file only in area2
            if (!area1.containsKey(filePath)) {
                this.orphanFiles2.add(filePath);
            }
        }
    }

    public void applyDiff(
            Consumer<String> orphan1Action,
            Consumer<String> orphan2Action,
            Consumer<String> modifyAction) {
        if (!this.orphanFiles1.isEmpty()) {       // file in index, not in commit, then insert
            for (String filePath : this.orphanFiles1) {
                orphan1Action.accept(filePath);
            }
        }
        if (!this.orphanFiles2.isEmpty()) {       // file in commit, not in index, then remove
            for (String filePath : orphanFiles2) {
                orphan2Action.accept(filePath);
            }
        }
        if (!this.modifiedFiles.isEmpty()) {      // file different versions, then update in commit
            for (String filePath : this.modifiedFiles) {
                modifyAction.accept(filePath);
            }
        }
    }
}
