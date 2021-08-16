package edu.northeastern.gitlet.command.plumbing;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

public class CatFileCommand extends GitletCommand {

    public CatFileCommand(Repository repo) {
        super(repo, 2, 2);
    }

    @Override
    protected String doExecute(String[] operands) {
        String type = operands[0];
        String hash = operands[1];
        switch (type) {
            case "-p":
                return this.getRepo().readFile(hash);
            case "-t":
                return this.getRepo().readFileType(hash);
            default:
                throw new GitletException("error: unknown switch " + type, true);
        }
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
