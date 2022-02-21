package edu.northeastern.gitlet.domain;

import edu.northeastern.gitlet.util.Utils;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;

/***
 * The class to read and write into .gitlet/objects storage
 */
public class ObjectStore {

    private static final File OBJECTS_DIR = Utils.join(Repository.GITLET_DIR, "objects");

    public void initObjectStore(){
        OBJECTS_DIR.mkdir();
    }

    public String hashObject(ObjectType objectType, Object o) {
        return this.hashObject(objectType, o, true);
    }

    public String hashObject(ObjectType objectType, Object o, boolean writeToObjects) {
        if (!(o instanceof String) && !(o instanceof byte[]) && (o instanceof Serializable)) {
            o = Utils.serialize((Serializable) o);
        }

        String objectHash = Utils.sha1(
                objectType.name(),
                " ",
                Integer.valueOf(Utils.getContentLength(o)).toString(),
                "\u0000",
                o);

        if (writeToObjects) {
            File filePath = Utils.join(OBJECTS_DIR, objectHash.substring(0, 2), objectHash.substring(2));
            if (!filePath.exists()) {
                Utils.writeContents(
                        filePath,
                        objectType.name(),
                        " ",
                        Integer.valueOf(Utils.getContentLength(o)).toString(),
                        "\u0000",
                        o);
            }
        }

        return objectHash;
    }

    public String readFileType(String operand) {
        File file = Utils.join(OBJECTS_DIR, operand.substring(0, 2), operand.substring(2));
        if (file.exists()) {
            return Utils.readContentsAsString(file).split("\u0000")[0].split(" ")[0];
        }

        return null;
    }

    public Object readObject(String hash) {
        File file = Utils.join(OBJECTS_DIR, hash.substring(0, 2), hash.substring(2));

        if (file.exists()) {
            String[] parts = Utils.readContentsAsString(file).split("\u0000");
            ObjectType objectType = ObjectType.valueOf(parts[0].split(" ")[0]);

            switch (objectType){
                case blob:
                    return parts.length == 1 ? "" : parts[1];
                case commit:
                    Commit commit = Utils.deserialize(parts[1], Commit.class);
                    return commit;
                case tree:
                    HashMap<String, TreeNode> tree = Utils.deserialize(parts[1], HashMap.class);
                    return tree;
            }
        }

        return null;
    }
}
