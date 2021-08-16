package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.Repository;

public class InitCommand extends GitletCommand {
    public InitCommand(Repository repo) {
        super(repo, 0, 0);
    }

    @Override
    public String doExecute(String[] operands) {
        Repository repo = this.getRepo();
        return repo.initRepo();
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
