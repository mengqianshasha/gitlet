package edu.northeastern.gitlet.domain;

import java.io.File;

public enum ConfigScope {
    Global(Repository.GLOBAL_CONFIG_FILE.getAbsolutePath()),
    Repo(Repository.CONFIG_FILE.getAbsolutePath());

    private String path;
    ConfigScope(String path){
        this.path = path;
    }

    public File getConfigLocation(){
        return new File(this.path);
    }
}
