package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.exception.GitletException;
import edu.northeastern.gitlet.util.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/***
 * The class to provide configuration functionality for gitlet
 */
public class ConfigStore {
    public static final File CONFIG_FILE = Utils.join(Repository.GITLET_DIR, "config");

    public static final File GLOBAL_CONFIG_FILE = Utils.join(
            new File(System.getProperty("user.home")), ".gitletconfig");

    public void initConfigStore() {
        try {
            CONFIG_FILE.createNewFile();
        } catch (IOException e) {
            throw new GitletException("Unexpected error:" + e);
        }
    }

    public void updateConfig(File file, String propertyName, String propertyValue){
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(file));
            properties.setProperty(propertyName, propertyValue);
            properties.store(new FileOutputStream(file), null);
        } catch (IOException e) {
            throw new GitletException(e.getMessage());
        }
    }

    public String getConfigValue(String key){
        Properties properties = this.loadConfigs();
        return properties.getProperty(key);
    }

    public Properties loadConfigs(){
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(ConfigScope.Global.getConfigLocation()));
        } catch (IOException e) {
            throw new GitletException(e.getMessage());
        }

        try {
            properties.load(new FileInputStream(ConfigScope.Repo.getConfigLocation()));
        } catch (IOException e) {
            //ignore exception if repo specific config file doesn't exist yet
        }

        return properties;
    }
}
