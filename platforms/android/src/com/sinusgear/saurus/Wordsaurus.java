/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
 */

package com.sinusgear.saurus;

import android.os.Bundle;
import android.util.Log;
import org.apache.cordova.*;

import java.io.*;

public class Wordsaurus extends CordovaActivity 
{
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

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        super.init();
        try
        {
            File dbFile = getDatabasePath("vks.db");
            if(!dbFile.exists()){
                Log.w("com.sinusgear.saurus", "installing DB");
                this.copy("/mnt/sdcard/Download/vks.db",dbFile.getAbsolutePath());
            } else {
                Log.w("com.sinusgear.saurus", "db file is already in place");
            }
        }
        catch (Exception e)
        {
            Log.w("com.sinusgear.saurus", "unable to copy db");
            e.printStackTrace();
        }

        // Set by <content src="index.html" /> in config.xml
        super.loadUrl(Config.getStartUrl());
        //super.loadUrl("file:///android_asset/www/index.html")
    }
}

