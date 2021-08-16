package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class CheckoutCommand extends GitletCommand {
    public CheckoutCommand(Repository repo) {
        super(repo, 1, 3);
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
