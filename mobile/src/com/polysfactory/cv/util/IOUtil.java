package com.polysfactory.cv.util;

import android.content.Context;
import android.util.Log;

import com.polysfactory.opencvocr.L;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class IOUtil {
    public static File getFilePath(Context context, String dirname, String filename) {
        File cascadeDir = context.getDir(dirname, Context.MODE_PRIVATE);
        return new File(cascadeDir, filename);
    }

    public static boolean copy(Context context, int res, File file) {
        InputStream is = context.getResources().openRawResource(res);
        FileOutputStream os = null;
        try {
            os = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            Log.e(L.TAG, "file copy error", e);
            return false;
        } catch (IOException e) {
            Log.e(L.TAG, "file copy error", e);
            return false;
        } finally {
            try {
                is.close();
                os.close();
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
