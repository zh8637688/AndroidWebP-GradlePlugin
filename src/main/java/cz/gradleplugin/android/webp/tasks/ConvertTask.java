package cz.gradleplugin.android.webp.tasks;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.internal.api.DefaultAndroidSourceSet;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.os.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import cz.gradleplugin.android.webp.util.Logger;
import cz.gradleplugin.android.webp.WebPAndroidPlugin;

/**
 * @author haozhou
 */

public class ConvertTask extends DefaultTask {
    private int quality;
    private boolean autoConvert;
    private String cwebpPath;
    private String projectRootPath;
    private Collection<File> drawableDirs;

    @Input
    public int getQuality() {
        return quality;
    }

    @Input
    public boolean isAutoConvert() {
        return autoConvert;
    }

    @InputFiles
    public Collection<File> getDrawableDirs() {
        return drawableDirs;
    }

    /*@TaskAction
    public void convert(IncrementalTaskInputs inputs) {
        if (!inputs.isIncremental()) {
            cleanPreOutput();
        }

        inputs.outOfDate(inputFileDetails -> {

        });

        inputs.removed(inputFileDetails -> {

        });
    }*/

    @TaskAction
    public void convert() throws Exception {
        getPermissionForMac();

        for (File drawableDir: drawableDirs) {
            File[] drawableFiles = drawableDir.listFiles();
            if (drawableFiles != null) {
                for (File drawableFile : drawableFiles) {
                    if (canConvert(drawableFile)) {
                        String srcName = drawableFile.getName();
                        int dotIndex = srcName.lastIndexOf(".");
                        String dstName = srcName.substring(0, dotIndex) + ".webp";

                        File parent = drawableFile.getParentFile();

                        String srcFilePath = drawableFile.getAbsolutePath();
                        String dstFilePath;
                        if (isAutoConvert()) {
                            dstFilePath = parent.getAbsolutePath() + "/" + dstName;
                        } else {
                            dstFilePath = projectRootPath + "/webp/" + parent.getName() + "/" + dstName;
                            File dstFile = new File(dstFilePath);
                            if (!dstFile.getParentFile().exists()) {
                                dstFile.getParentFile().mkdirs();
                            }
                        }

                        try {
                            String script = cwebpPath + " -q " + getQuality() + " "
                                    + srcFilePath + " -o " + dstFilePath;
                            Process process = Runtime.getRuntime().exec(script);
                            if (process.waitFor() != 0) {
                                Logger.i("convert failed: " + srcFilePath);
                                continue;
                            }
                            if (isAutoConvert()) {
                                moveFileTo(srcFilePath, projectRootPath + "/ori_res/" + parent.getName() + "/" + srcName);
                            }
                        } catch (IOException e) {
                            Logger.i("convert failed: " + srcFilePath);
                        }
                    }
                }
            }
        }
    }

    private boolean canConvert(File drawableFile) {
        String fileName = drawableFile.getName();
        return (fileName.endsWith(".png") && !fileName.contains(".9"))
                || fileName.endsWith(".jpg");
    }

    private void getPermissionForMac() throws Exception{
        OperatingSystem os = OperatingSystem.current();
        if (os.isMacOsX()) {
            String script = "chmod a+x " + cwebpPath;
            Process process = Runtime.getRuntime().exec(script);
            if (process.waitFor() != 0) {
                throw new Exception("Can not get executive permission, please execute 'chmod a+x " + cwebpPath + "' on terminal");
            }
        }
    }

    private void moveFileTo(String src, String dst) {
        File dstFile = new File(dst);
        if (!dstFile.getParentFile().exists()) {
            dstFile.getParentFile().mkdirs();
        }
        new File(src).renameTo(dstFile);
    }

    public static class ConfigAction implements Action<ConvertTask> {
        private Project project;
        private String cwebpPath;
        private boolean autoConvert;
        private int quality;

        public ConfigAction(Project project, String cwebpPath, boolean autoConvert, int quality) {
            this.project = project;
            this.cwebpPath = cwebpPath;
            this.autoConvert = autoConvert;
            this.quality = quality;
        }

        @Override
        public void execute(ConvertTask convertTask) {
            Collection<File> androidResDirs = getAndroidResDirectories(project);
            convertTask.drawableDirs = getDrawableDirsFromRes(androidResDirs);
            convertTask.projectRootPath = project.getProjectDir().getAbsolutePath();
            convertTask.cwebpPath = cwebpPath;
            convertTask.autoConvert = autoConvert;
            convertTask.quality = quality;
        }

        private Collection<File> getDrawableDirsFromRes(Collection<File> resDirs) {
            List<File> drawableDirs = new ArrayList<>();
            if (resDirs != null) {
                Pattern pattern = Pattern.compile("^drawable.*|^mipmap.*");
                for (File resDir : resDirs) {
                    File[] subResDirs = resDir.listFiles();
                    if (subResDirs != null) {
                        for (File subResDir : subResDirs) {
                            if (subResDir != null) {
                                String dirName = subResDir.getName();
                                if (pattern.matcher(dirName).matches()) {
                                    drawableDirs.add(subResDir);
                                }
                            }
                        }
                    }
                }
            }
            return drawableDirs;
        }

        private Collection<File> getAndroidResDirectories(Project project) {
            DefaultAndroidSourceSet sourceSet = null;
            Object androidExtension = project.getExtensions().findByName("android");
            if (project.getPlugins().hasPlugin(WebPAndroidPlugin.APP_PLUGIN)) {
                AppExtension appExtension = (AppExtension) androidExtension;
                String name = appExtension.getDefaultConfig().getName();
                sourceSet = (DefaultAndroidSourceSet) appExtension.getSourceSets().getByName(name);
            } else if (project.getPlugins().hasPlugin(WebPAndroidPlugin.LIB_PLUGIN)) {
                LibraryExtension libraryExtension = (LibraryExtension) androidExtension;
                String name = libraryExtension.getDefaultConfig().getName();
                sourceSet = (DefaultAndroidSourceSet) libraryExtension.getSourceSets().getByName(name);
            }

            if (sourceSet != null) {
                return sourceSet.getResDirectories();
            }
            return null;
        }
    }
}
