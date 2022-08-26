package edu.northeastern.gitlet;

import edu.northeastern.gitlet.command.*;
import edu.northeastern.gitlet.command.plumbing.*;
import edu.northeastern.gitlet.command.porcelain.*;
import edu.northeastern.gitlet.domain.Repository;
import edu.northeastern.gitlet.exception.GitletException;

import java.util.Arrays;

public class Gitlet {
    public static void main(String[] args) {
        Gitlet gitlet = new Gitlet();
        gitlet.run(args);
    }

    public void run(String[] args) {
        // If a user doesn't input any arguments, print the message "Please enter a command." and exit.
        if (args.length == 0) {
            System.out.println("Please enter a command");
            System.exit(1);
        }

        String firstArg = args[0];
        String[] operands = new String[0];
        if (args.length > 1) {
             operands = Arrays.copyOfRange(args, 1, args.length);
        }

        GitletCommand cmd = null;
        try {
            cmd = getCommand(firstArg);
            String result = cmd.execute(operands);
            if (result != null) {
                System.out.println(result);
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            if (e.GetShowUsageMessage()){
                System.out.println(cmd.getUsageMessage());
            }
            System.exit(0);
        }
    }

    private GitletCommand getCommand(String arg) {
        GitletCommand cmd = null;
        switch(arg) {
            case "ls-files":
                cmd = new ListFilesCommand(new Repository());
                break;
            case "ls-tree":
                cmd = new ListTreeCommand(new Repository());
                break;
            case "cat-file":
                cmd = new CatFileCommand(new Repository());
                break;
            case "hash-object":
                cmd = new HashObjectCommand(new Repository());
                break;
            case "rev-parse":
                cmd = new RevParseCommand(new Repository());
                break;
            case "add":
                cmd = new AddCommand(new Repository());
                break;
            case "branch":
                cmd = new BranchCommand(new Repository());
                break;
            case "rm-branch":
                cmd = new RemoveBranchCommand(new Repository());
                break;
            case "checkout":
                cmd = new CheckoutCommand(new Repository());
                break;
            case "commit":
                cmd = new CommitCommand(new Repository());
                break;
            case "config":
                cmd = new ConfigCommand(new Repository());
                break;
            case "init":
                cmd = new InitCommand(new Repository());
                break;
            case "log":
                cmd = new LogCommand(new Repository());
                break;
            case "rm":
                cmd = new RemoveCommand(new Repository());
                break;
            case "reset":
                cmd = new ResetCommand(new Repository());
                break;
            case "status":
                cmd = new StatusCommand(new Repository());
                break;
            default:
                throw new GitletException("No command with that name exists");
        }

        return cmd;
    }
}
