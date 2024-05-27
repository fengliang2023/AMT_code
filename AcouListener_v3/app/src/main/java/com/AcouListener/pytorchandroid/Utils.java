package com.AcouListener.pytorchandroid;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Utils {
// 从app中读取mobilenet-v2.pt模型，并保存到本地文件，然后将被本地文件的地址输出
    public static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);    // 新建文件

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {  //打开资产文件的输入流，将数据写入文件
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();  //  如果一切顺利，返回文件的绝对路径
        } catch (IOException e) {
            Log.e("pytorchandroid", "Error process asset " + assetName + " to file path");
        }
        return null;
    }

}
