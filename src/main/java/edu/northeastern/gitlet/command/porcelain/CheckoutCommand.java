package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

public class CheckoutCommand extends GitletCommand {
    public CheckoutCommand(Repository repo) {
        super(repo, 1, 3);
    }

    @Override
    protected String doExecute(String[] operands) {
        if (operands.length == 1) {
            return this.getRepo().checkoutBranch(operands[0]);
        } else if (operands.length == 2) {
            return this.getRepo().checkoutFile(operands[1]);
        } else {
            return this.getRepo().checkoutFileFromCommit(operands[0], operands[2]);
        }
    }

    @Override
    public String getUsageMessage() {
        return null;
    }

    @Override
    protected void validateNumOperands(String[] args) {
        int len = args.length;
        if ((len == 2 && !args[0].equals("--")) || (len == 3 && !args[1].equals("--"))) {
            throw new GitletException("Incorrect operands");
        }
        super.validateNumOperands(args);
    }
}
