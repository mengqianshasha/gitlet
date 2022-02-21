package edu.northeastern.gitlet.command;

import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

/**
 * The abstract class for exposing a new git command
 */
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

    protected void validateNumOperands(String[] args) {
        if (args.length > this.maxNumOfOperands || args.length < this.minNumOfOperands) {
            throw new GitletException("Incorrect operands.");
        }
    }
}
