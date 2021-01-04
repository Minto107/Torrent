package res;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class TorrentFile is used for storing information like MD5 hash and file location for files that
 * are stored on the file server.
 */
public class TorrentFile {
    String name, md5, fileLocation;

    /**
     * Constructs new TorrentFile object
     *
     * @param fileLocation Required for object creation, specifies where desired file is located
     */

    public TorrentFile(String fileLocation) {
        Pattern pattern = Pattern.compile("[\\\\]{0}[\\w-.]*$");
        Matcher matcher = pattern.matcher(fileLocation);
        matcher.find();
        name = matcher.group(0);
        this.fileLocation = fileLocation;
        md5 = generateMD5(fileLocation);
    }

    /**
     * @param fileLocation MD5 will be generated for this file
     * @return Returns MD5 for specified file
     * @brief Generates MD5 for specified file
     */

    private String generateMD5(String fileLocation) {
        try {
            FileInputStream fis = new FileInputStream(fileLocation);
            byte[] bytes = new byte[1024];
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            int numRead;
            do {
                numRead = fis.read(bytes);
                if (numRead > 0) {
                    md5.update(bytes, 0, numRead);
                }
            } while (numRead != -1);
            byte[] b = md5.digest();
            StringBuilder sb = new StringBuilder();
            for (byte value : b) {
                sb.append(Integer.toString((value & 0xff) + 0x100, 16).substring(1));
            }
            fis.close();
            return sb.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return Returns String with details about specified file
     * @brief Overloaded toString() method returns details about specified file in more user-friendly way
     */

    @Override
    public String toString() {
        return "File name = " + name + ", md5 = " + md5 + ", file location = " + fileLocation;
    }

}
