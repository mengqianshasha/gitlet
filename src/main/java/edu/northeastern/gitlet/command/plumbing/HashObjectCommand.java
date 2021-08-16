package edu.northeastern.gitlet.command.plumbing;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.ObjectType;
import edu.northeastern.gitlet.domain.Repository;

public class HashObjectCommand extends GitletCommand {
    public HashObjectCommand(Repository repo) {
        super(repo, 1, 1);
    }

    @Override
    protected String doExecute(String[] operands) {
        return this.getRepo().hashObject(ObjectType.blob, operands[0]);
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
