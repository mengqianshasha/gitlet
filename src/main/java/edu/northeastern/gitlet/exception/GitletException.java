package edu.northeastern.gitlet.exception;

/** General exception indicating a Gitlet error.  For fatal errors, the
 *  result of .getMessage() is the error message to be printed.
 *  @author P. N. Hilfinger
 */
public class GitletException extends RuntimeException {
    private boolean showUsageMessage;

    public GitletException(String msg){
        this(msg, false);
    }

    /** A GitletException MSG as its message. */
    public GitletException(String msg, boolean showUsageMessage) {
        super(msg);
        this.showUsageMessage = showUsageMessage;
    }

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method. */
    public static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    public boolean GetShowUsageMessage(){
        return showUsageMessage;
    }
}
