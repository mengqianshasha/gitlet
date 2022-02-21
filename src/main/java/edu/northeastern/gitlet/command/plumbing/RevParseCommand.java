package edu.northeastern.gitlet.command.plumbing;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class RevParseCommand extends GitletCommand {
    public RevParseCommand(Repository repo) {
        super(repo, 1, 1);
    }

    @Override
    protected String doExecute(String[] operands) {
        return this.getRepo().parseReference(operands[0]);
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
