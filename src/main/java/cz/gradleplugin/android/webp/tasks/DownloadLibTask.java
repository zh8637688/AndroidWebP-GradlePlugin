package cz.gradleplugin.android.webp.tasks;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import cz.gradleplugin.android.webp.util.Logger;

/**
 * @author haozhou
 */

public class DownloadLibTask extends DefaultTask {
    private String downloadUrl;
    private String downloadFilePath;

    @Input
    public String getDownloadFilePath() {
        return downloadFilePath;
    }

    @OutputFile
    public File getDownloadFile() {
        return new File(downloadFilePath);
    }

    @TaskAction
    public void downloadLib() throws Exception {
        if (downloadUrl != null && isFilePathValid(downloadFilePath)
                && !new File(downloadFilePath).exists()) {
            BufferedInputStream bis = null;
            BufferedOutputStream bos = null;
            try {
                Logger.i("Start download lib of WebP, " + downloadUrl);
                HttpURLConnection connection = (HttpURLConnection) new URL(downloadUrl).openConnection();
                connection.setConnectTimeout(30000);
                connection.setReadTimeout(30000);
                connection.connect();
                if (connection.getResponseCode() == 200) {
                    if (createDownloadFileIfNeed()) {
                        bis = new BufferedInputStream(connection.getInputStream());
                        bos = new BufferedOutputStream(new FileOutputStream(downloadFilePath));
                        int length;
                        byte[] bytes = new byte[1024];
                        while ((length = bis.read(bytes)) != -1) {
                            bos.write(bytes, 0, length);
                        }
                        bos.flush();
                        Logger.i("Download finished.");
                    } else {
                        throw new Exception("Download failed, can not create download file.");
                    }
                } else {
                    throw new Exception("Download failed, request has been refused.");
                }
            } finally {
                if (bis != null) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (bos != null) {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private boolean createDownloadFileIfNeed() {
        File file = new File(downloadFilePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            if (!parent.mkdirs()) {
                return false;
            }
        }
        if (file.exists() && !file.delete()) {
            return false;
        }
        try {
            if (!file.createNewFile()) {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private boolean isFilePathValid(String path) {
        if (path != null) {
            File dir = new File(path).getParentFile();
            return dir.isDirectory() && dir.canWrite();
        } else {
            return false;
        }
    }

    public static class ConfigAction implements Action<DownloadLibTask> {
        private String downloadUrl;
        private String downloadFilePath;

        public ConfigAction(String downloadUrl, String downloadFilePath) {
            this.downloadUrl = downloadUrl;
            this.downloadFilePath = downloadFilePath;
        }

        @Override
        public void execute(DownloadLibTask downloadLibTask) {
            downloadLibTask.downloadUrl = downloadUrl;
            downloadLibTask.downloadFilePath = downloadFilePath;
        }
    }
}
