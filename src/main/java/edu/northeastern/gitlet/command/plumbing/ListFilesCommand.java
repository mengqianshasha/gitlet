package edu.northeastern.gitlet.command.plumbing;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

public class ListFilesCommand extends GitletCommand {

    public ListFilesCommand(Repository repo) {
        super(repo, 0, 1);
    }

    @Override
    protected String doExecute(String[] operands) {
        if (operands.length == 0) {
           return this.getRepo().listFilesFromCWD();
        }

        if (operands[0].equals("-s")) {
            return this.getRepo().listFilesFromIndex();
        }

        throw new GitletException("error: unknown switch " + operands[0], true);
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
