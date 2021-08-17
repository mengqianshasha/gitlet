package edu.northeastern.gitlet.command;

import edu.northeastern.gitlet.domain.Repository;

public abstract class GitletCommand {
    private Repository repo;
    private int minNumOfOperands;
    private int maxNumOfOperands;

    public GitletCommand(Repository repo, int minNumOfOperands, int maxNumberOfOperands) {
        this.repo = repo;
        this.minNumOfOperands = minNumOfOperands;
        this.maxNumOfOperands = maxNumberOfOperands;
    }

    protected Repository getRepo() {
        return repo;
    }

    public String execute(String[] args) {
        this.validateNumOperands(args);
        return doExecute(args);
    }

    protected abstract String doExecute(String[] operands);

    public abstract String getUsageMessage();

    /**
     * Helper method: validate the number of operands in the arguments provided by the user
     * If a user inputs a command with the wrong number or format of operands,
     * print the message "Incorrect operands." and exit.
     * @param args The arguments provided by the user in the command line
     */
    protected void validateNumOperands(String[] args) {
        if (args.length > this.maxNumOfOperands || args.length < this.minNumOfOperands) {
            System.out.println("Incorrect operands.");
            System.exit(0);
        }
    }
}
