package cz.webp.tasks;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import cz.webp.util.Logger;

/**
 * @author haozhou
 */

public class DecompressTask extends DefaultTask {
    private String srcFilePath;
    private String dstDirPath;

    @InputFile
    public File getSrcFile() {
        return new File(srcFilePath);
    }

    @OutputDirectory
    public File getDstDir() {
        return new File(dstDirPath);
    }

    @TaskAction
    public void decompressFile() throws Exception {
        Logger.i("Start decompress file " + srcFilePath);
        if (srcFilePath.endsWith("tar.gz")) {
            decompressGzip(srcFilePath);
        } else {
            decompressZip(srcFilePath);
        }
        Logger.i("Decompress finished.");
    }

    private void decompressGzip(String gzipFilePath) throws Exception {
        TarArchiveInputStream tis = null;
        BufferedOutputStream bos = null;
        try {
            File gzipFile = new File(gzipFilePath);
            tis = new TarArchiveInputStream(
                    new GZIPInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(gzipFile))));
            TarArchiveEntry tae;
            String fileFolder = gzipFile.getParentFile().getAbsolutePath();
            while ((tae = tis.getNextTarEntry()) != null) {
                File tmpFile = new File(fileFolder, tae.getName());
                if (tae.isDirectory()) {
                    tmpFile.mkdirs();
                } else {
                    if (!tmpFile.getParentFile().exists()) {
                        tmpFile.getParentFile().mkdirs();
                    }
                    try {
                        bos = new BufferedOutputStream(new FileOutputStream(tmpFile));
                        int length;
                        byte[] b = new byte[1024];
                        while ((length = tis.read(b)) != -1) {
                            bos.write(b, 0, length);
                        }
                    } finally {
                        try {
                            if (bos != null) {
                                bos.close();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } finally {
            if (tis != null) {
                try {
                    tis.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void decompressZip(String zipFilePath) throws Exception {
        ZipFile zipFile = null;
        try {
            zipFile = new ZipFile(zipFilePath);
            Enumeration<? extends ZipEntry> enums = zipFile.entries();
            String fileFolder = new File(zipFilePath).getParentFile().getAbsolutePath();
            while (enums.hasMoreElements()) {
                ZipEntry entry = enums.nextElement();
                File tmpFile = new File(fileFolder, entry.getName());
                if (entry.isDirectory()) {
                    tmpFile.mkdirs();
                } else {
                    if (!tmpFile.getParentFile().exists()) {
                        tmpFile.getParentFile().mkdirs();
                    }

                    InputStream in = null;
                    OutputStream out = null;
                    try {
                        in = zipFile.getInputStream(entry);
                        out = new FileOutputStream(tmpFile);
                        int length;
                        byte[] b = new byte[1024];
                        while ((length = in.read(b)) != -1) {
                            out.write(b, 0, length);
                        }

                    } finally {
                        if (in != null) {
                            try {
                                in.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (out != null) {
                            try {
                                out.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ConfigAction implements Action<DecompressTask> {
        private String downloadFilePath;

        public ConfigAction(String downloadFilePath) {
            this.downloadFilePath = downloadFilePath;
        }

        @Override
        public void execute(DecompressTask decompressTask) {
            decompressTask.srcFilePath = downloadFilePath;
            decompressTask.dstDirPath = new File(downloadFilePath).getParent();
        }
    }
}
