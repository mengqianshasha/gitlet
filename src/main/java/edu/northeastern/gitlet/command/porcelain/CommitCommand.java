package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

public class CommitCommand extends GitletCommand {
    public CommitCommand(Repository repo) {
        super(repo, 1, 1);
    }

    @Override
    protected String doExecute(String[] operands) {
        return null;
    }

    @Override
    public String getUsageMessage() {
        return null;
    }

    @Override
    protected void validateNumOperands(String[] args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a commit message.");
        }
        super.validateNumOperands(args);
    }
}
