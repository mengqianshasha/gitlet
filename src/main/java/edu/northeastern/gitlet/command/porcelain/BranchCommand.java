package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class BranchCommand extends GitletCommand {
    public BranchCommand(Repository repo) {
        super(repo, 1, 1);
    }

    @Override
    protected String doExecute(String[] operands) {
        return this.getRepo().branch(operands[0]);
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
