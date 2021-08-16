package edu.northeastern.gitlet.command.plumbing;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class ListTreeCommand extends GitletCommand {
    public ListTreeCommand(Repository repo) {
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
}
