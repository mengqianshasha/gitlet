package edu.northeastern.gitlet.util;

import edu.northeastern.gitlet.exception.GitletException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Formatter;
import java.util.List;

/**
 * Common util class needed by gitlet
 */
public class Utils {

    static final int UID_LENGTH = 40;

    public static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    public static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    public static int getContentLength(Object val){
        if (val instanceof byte[]) {
            return ((byte[]) val).length;
        } else if (val instanceof String) {
            return ((String) val).getBytes(StandardCharsets.UTF_8).length;
        } else {
            throw new IllegalArgumentException("improper type to getContentLength");
        }
    }

    public static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    public static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }

            if (!file.getParentFile().exists()){
                file.getParentFile().mkdirs();
            }

            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        return deserialize(readContents(file), expectedClass);
    }

    public static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    public static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    public static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    public static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    public static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }

    public static <T extends Serializable> T deserialize(String byteStr, Class<T> expectedClass) {
        return deserialize(byteStr.getBytes(StandardCharsets.UTF_8), expectedClass);
    }

    public static <T extends Serializable> T deserialize(byte[] byteArr, Class<T> expectedClass) {
        try {
            final byte[] bytes = Base64.getDecoder().decode(byteArr);
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            ObjectInput in = new ObjectInputStream(bis);
            T result =  expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            // Base64 encoding is mandatory otherwise some bit information will lost after convert this byte using
            // new String(byteArray, StandardCharsets.UTF_8)
            return Base64.getEncoder().encodeToString(stream.toByteArray()).getBytes(StandardCharsets.UTF_8);
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static String[] splitPath(String filePath){
        return filePath.split(File.separator);
    }
}
