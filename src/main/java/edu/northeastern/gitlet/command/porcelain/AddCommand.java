package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class AddCommand extends GitletCommand {

    public AddCommand(Repository repo) {
        super(repo, 1, 1);
    }

    @Override
    protected String doExecute(String[] operands) {
        return this.getRepo().add(operands[0]);
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
