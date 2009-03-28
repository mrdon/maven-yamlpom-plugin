package org.twdata.maven.yamlpom;

/**
 *
 */

import org.apache.commons.io.IOUtils;

import java.security.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MD5
{

    private MessageDigest md = null;
    private static final char[] hexChars = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    /**
     * Constructor is private so you must use the getInstance method
     */
    public MD5()
    {
        try
        {
            md = MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String hashFile(File fileToHash)
    {
        FileInputStream fin = null;
        try
        {
            fin = new FileInputStream(fileToHash);
            byte[] data = IOUtils.toByteArray(fin);
            return hexStringFromBytes((calculateHash(data)));
        }
        catch (FileNotFoundException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public String hashData(byte[] dataToHash)

    {

        return hexStringFromBytes((calculateHash(dataToHash)));
    }


    private byte[] calculateHash(byte[] dataToHash)

    {
        md.update(dataToHash, 0, dataToHash.length);

        return (md.digest());
    }


    public String hexStringFromBytes(byte[] b)

    {

        String hex = "";

        int msb;

        int lsb = 0;
        int i;

        // MSB maps to idx 0

        for (i = 0; i < b.length; i++)

        {

            msb = ((int) b[i] & 0x000000FF) / 16;

            lsb = ((int) b[i] & 0x000000FF) % 16;
            hex = hex + hexChars[msb] + hexChars[lsb];
        }
        return (hex);
    }
}