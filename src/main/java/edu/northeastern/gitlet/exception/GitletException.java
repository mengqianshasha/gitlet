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

    public static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    public boolean GetShowUsageMessage(){
        return showUsageMessage;
    }
}
