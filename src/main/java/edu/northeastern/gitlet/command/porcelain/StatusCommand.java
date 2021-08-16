package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class StatusCommand extends GitletCommand {
    public StatusCommand(Repository repo) {
        super(repo, 0, 0);
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
