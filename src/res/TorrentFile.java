package res;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TorrentFile {
    public String name, md5, fileLocation;

    public TorrentFile(String name, String fileLocation){
        this.name = name;
        this.fileLocation = fileLocation;
        md5 = generateMD5(fileLocation);
    }

    public TorrentFile(String name, String md5, String fileLocation){
        this.name = name;
        this.md5 = md5;
        this.fileLocation = fileLocation;
    }

    public TorrentFile(String fileLocation) {
        this.fileLocation = fileLocation;
        md5 = generateMD5(fileLocation);
        Pattern pattern = Pattern.compile("[\\\\]{0}[\\w-.]*$");
        Matcher matcher = pattern.matcher(fileLocation);
        matcher.find();
        name = matcher.group(0);
    }

    String generateMD5(String fileLocation){
        try {
            FileInputStream fis = new FileInputStream(fileLocation);
            byte[] buffer = new byte[1024];
            MessageDigest complete = MessageDigest.getInstance("MD5");
            int numRead;

            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);

            byte[] b = complete.digest();
            String result = "";

            for (int i=0; i < b.length; i++) {
                result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
            }
            fis.close();
            return result;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return  "File name = " + name + ", md5 = " + md5 + ", file location = " + fileLocation;
    }

}
