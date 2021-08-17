package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class LogCommand extends GitletCommand {
    public LogCommand(Repository repo) {
        super(repo, 0, 0);
    }

    @Override
    protected String doExecute(String[] operands) {
        return this.getRepo().log();
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
