package me.racer.jjj.storagemanagerapplication.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import static me.racer.jjj.storagemanagerapplication.StorageManagerApplication.IMG_FILE_PATH;

public class imageutils {

    // Credit to "https://stackoverflow.com/questions/10292792/getting-image-from-url-java"

    public static String saveImage(String imgurl) throws IOException {
        URL url = new URL(imgurl);
        InputStream is = url.openStream();
        String[] imgname = imgurl.split("/");
        OutputStream os = new FileOutputStream(IMG_FILE_PATH + imgname[imgname.length-1]);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }
        is.close();
        os.close();
        return IMG_FILE_PATH + imgname[imgname.length-1];
    }

}
