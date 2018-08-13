package com.fencedin.backend.rest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * File utilities
 */
public class FileUtils {

    /**
     * File read utilities
     */
    public static final class ReadUtils {

        /**
         * Create a full ByteArrayInputStream from a file.
         * @param filePath The path of the file
         * @throws IOException If any of the streams could not be created
         * @author (http://knowledge-oracle.blogspot.com/2009/02/import-private-key-and-certificate-in.html)
         **/
        public static ByteArrayInputStream createFullByteArrayInputStreamFromFile(String filePath) throws IOException {
            FileInputStream fileFileInputStrm = new FileInputStream(filePath);
            DataInputStream fileDataInputStrm = new DataInputStream(fileFileInputStrm);
            byte[] fileData = new byte[fileDataInputStrm.available()];

            fileDataInputStrm.readFully(fileData);
            return new ByteArrayInputStream(fileData);
        }

    }

}
