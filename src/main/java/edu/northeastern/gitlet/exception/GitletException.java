package edu.northeastern.gitlet.exception;

public class GitletException extends RuntimeException {
    private boolean showUsageMessage;

    public GitletException(String msg){
        this(msg, false);
    }

    public GitletException(String msg, boolean showUsageMessage) {
        super(msg);
        this.showUsageMessage = showUsageMessage;
    }

    public boolean GetShowUsageMessage(){
        return showUsageMessage;
    }
}
