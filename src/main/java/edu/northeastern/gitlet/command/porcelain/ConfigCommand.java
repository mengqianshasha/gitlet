package edu.northeastern.gitlet.command.porcelain;

import edu.northeastern.gitlet.command.GitletCommand;
import edu.northeastern.gitlet.domain.ConfigScope;
import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

public class ConfigCommand extends GitletCommand {
    public ConfigCommand(Repository repo) {
        super(repo, 1, 3);
    }

    @Override
    protected String doExecute(String[] operands) {
        ConfigScope scope = null;
        String propertyName = null;
        String propertyValue = null;
        if (operands.length == 1){
            if (operands[0].equals("--list")) {
                return this.getRepo().listConfig();
            }else{
                throw new GitletException("error: unknown switch " + operands[0], true);
            }
        } else if(operands.length == 2) {
            scope = ConfigScope.Repo;
            propertyName = operands[0];
            propertyValue = operands[1];
        } else if (operands.length == 3) {
            if (operands[0].equals("--global")){
                scope = ConfigScope.Global;
            } else{
                throw new GitletException("error: unknown switch " + operands[0], true);
            }

            propertyName = operands[1];
            propertyValue = operands[2];
        }

        return this.getRepo().config(scope.getConfigLocation(), propertyName, propertyValue);
    }

    @Override
    public String getUsageMessage() {
        return null;
    }
}
