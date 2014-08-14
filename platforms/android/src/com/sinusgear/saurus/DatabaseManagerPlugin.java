package com.sinusgear.saurus;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.util.Log;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;

/**
 * Created by georgik on 14.8.2014.
 */
public class DatabaseManagerPlugin extends CordovaPlugin {

    void copy(String file, String folder) throws IOException
    {
        File CheckDirectory;
        CheckDirectory = new File(folder);

        String parentPath = CheckDirectory.getParent();

        File filedir = new File(parentPath);
        if (!filedir.exists()) {
            if (!filedir.mkdirs()) {
                return;
            }
        }

        File inputFile = new File(file);
        if (inputFile.exists()) {
            Log.w("com.sinusgear.saurus", "source file exists");
        } else {
            Log.w("com.sinusgear.saurus", "source file does not exist");
        }
        InputStream in = new FileInputStream(inputFile);

        File newfile = new File(folder);
        OutputStream out = new FileOutputStream(newfile);

        byte[] buf = new byte[1024];
        int len; while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        in.close(); out.close();
    }

    public boolean loadDatabase(String pathToSourceFile) {
        try {
            Context context=this.cordova.getActivity().getApplicationContext();
            String sourcePath = Uri.parse(pathToSourceFile).getPath();
            File dbFile = context.getDatabasePath("vks.db");
            Log.w("com.sinusgear.saurus", "installing DB");
            Log.w("com.sinusgear.saurus", sourcePath);
            this.copy(sourcePath, dbFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.w("com.sinusgear.saurus", "unable to copy db");
            e.printStackTrace();
        }
        return false;
    }


    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.w("com.sinusgear.saurus", action);
        if ("load".equals(action)) {
            this.loadDatabase(args.getString(0));
            callbackContext.success();
            return true;
        } else if ("exists".equals(action)) {
            if (this.cordova.getActivity().getDatabasePath(args.getString(0) + ".db").exists()) {
                callbackContext.success();
            } else {
                callbackContext.error("none");
            }
            return true;
        }
        return false;  // Returning false results in a "MethodNotFound" error.
    }
}
