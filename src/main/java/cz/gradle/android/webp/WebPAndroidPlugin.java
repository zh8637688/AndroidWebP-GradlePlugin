package cz.gradle.android.webp;

import com.android.build.gradle.AppExtension;
import com.android.build.gradle.LibraryExtension;
import com.android.build.gradle.api.BaseVariant;
import com.android.build.gradle.internal.variant.BaseVariantData;
import com.android.utils.StringHelper;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.invocation.Gradle;
import org.gradle.internal.os.OperatingSystem;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import cz.gradle.android.webp.util.Logger;
import cz.gradle.android.webp.tasks.ConvertTask;
import cz.gradle.android.webp.tasks.DecompressTask;
import cz.gradle.android.webp.tasks.DownloadLibTask;

/**
 * @author haozhou
 */

public class WebPAndroidPlugin implements Plugin<Project> {
    public static final String APP_PLUGIN = "com.android.application";
    public static final String LIB_PLUGIN = "com.android.library";

    private static final String EXTENSIONS_NAME = "WebPAndroid";
    private static final String HOST_URL = "https://storage.googleapis.com/downloads.webmproject.org/releases/webp/";

    @Override
    public void apply(Project project) {
        project.getExtensions().create(EXTENSIONS_NAME, WebPAndroidExtension.class);
        Logger.initialize(project.getLogger());

        final String libFileName = getLibFileName();

        if (libFileName != null) {
            project.afterEvaluate(p -> {
                WebPAndroidExtension extension = p.getExtensions().findByType(WebPAndroidExtension.class);
                String downloadUrl = HOST_URL + libFileName;
                String downloadDir = getDefaultDownloadDir(p.getGradle());
                String downloadFilePath = downloadDir + "/" + libFileName;

                Task downloadTask = createDownloadTask(p, downloadUrl, downloadFilePath);
                Task decompressTask = createDecompressTask(p, downloadFilePath);

                List<String> variantsName = getVariantsName(p);
                for (String variantName : variantsName) {
                    Task convertTask = createConvertTask(p,
                            variantName, getCWebPPath(downloadDir),
                            extension.isAutoConvert(), extension.getQuality());
                    convertTask.dependsOn(decompressTask.dependsOn(downloadTask));

                    if (extension.isAutoConvert()) {
                        attachCovertTaskToBuild(p, variantName, convertTask);
                    }
                }
            });
        } else {
            Logger.i("Can not support your operating system.");
        }
    }

    private Task createDownloadTask(Project project, String downloadUrl, String downloadFilePath) {
        DownloadLibTask task = project.getTasks().create("downloadLibWebP", DownloadLibTask.class,
                new DownloadLibTask.ConfigAction(downloadUrl, downloadFilePath));
        task.setGroup("webp");
        return task;
    }

    private Task createDecompressTask(Project project, String downloadFilePath) {
        DecompressTask task = project.getTasks().create("decompressDownloadFile", DecompressTask.class,
                new DecompressTask.ConfigAction(downloadFilePath));
        task.setGroup("webp");
        return task;
    }

    private Task createConvertTask(Project project, String variant, String cwebpPath, boolean autoConvert, int quality) {
        ConvertTask task = project.getTasks().create("convert" + StringHelper.capitalize(variant) + "WebP",
                ConvertTask.class, new ConvertTask.ConfigAction(project, cwebpPath, autoConvert, quality));
        task.setGroup("webp");
        return task;
    }

    private void attachCovertTaskToBuild(Project project, String variant, Task convertTask) {
        String nameOfMergeResources = "merge" + StringHelper.capitalize(variant) + "Resources";
        Task mergeResources = project.getTasks().findByName(nameOfMergeResources);
        mergeResources.dependsOn(convertTask);
    }

    private String getDefaultDownloadDir(Gradle gradle) {
        File gradleUserHome = gradle.getGradleUserHomeDir();
        String dependencyCacheDir = "/caches/modules-2/files-2.1/";
        String packageName = "cz.webp";
        return gradleUserHome.getAbsolutePath() + dependencyCacheDir + packageName;
    }

    private String getLibFileName() {
        OperatingSystem os = OperatingSystem.current();
        if (os.isMacOsX()) {
            return "libwebp-1.0.0-rc3-mac-10.13.tar.gz";
        } else if (os.isWindows()) {
            String arch = System.getProperty("os.arch");
            if ("x86".equals(arch)) {
                return "libwebp-1.0.0-windows-x86-no-wic.zip";
            } else if ("x86_64".equals(arch)) {
                return "libwebp-1.0.0-windows-x64-no-wic.zip";
            }
        }
        return null;
    }

    private String getCWebPPath(String downloadDir) {
        String path = null;
        OperatingSystem os = OperatingSystem.current();
        if (os.isMacOsX()) {
            path = "libwebp-1.0.0-rc3-mac-10.13/bin/cwebp";
        } else if (os.isWindows()) {
            String arch = System.getProperty("os.arch");
            if ("x86".equals(arch)) {
                path = "libwebp-1.0.0-windows-x86-no-wic/bin/cwebp.exe";
            } else if ("x86_64".equals(arch)) {
                path = "libwebp-1.0.0-windows-x64-no-wic/bin/cwebp.exe";
            }
        }
        return downloadDir + "/" + path;
    }

    private List<String> getVariantsName(Project project) {
        List<String> variantsName = new ArrayList<>();
        Object androidExtension = project.getExtensions().findByName("android");
        if (project.getPlugins().hasPlugin(APP_PLUGIN)) {
            AppExtension appExtension = (AppExtension) androidExtension;
            appExtension.getApplicationVariants().forEach(applicationVariant -> {
                BaseVariantData variantData = getVariantData(applicationVariant);
                if (variantData != null) {
                    variantsName.add(variantData.getScope().getVariantConfiguration().getFullName());
                }
            });
        } else if (project.getPlugins().hasPlugin(LIB_PLUGIN)) {
            LibraryExtension libraryExtension = (LibraryExtension) androidExtension;
            libraryExtension.getLibraryVariants().forEach(libraryVariant -> {
                BaseVariantData variantData = getVariantData(libraryVariant);
                if (variantData != null) {
                    variantsName.add(variantData.getVariantConfiguration().getFullName());
                }
            });
        }
        return variantsName;
    }

    private BaseVariantData getVariantData(BaseVariant variant) {
        try {
            Method methodGetVariantData = variant.getClass().getMethod("getVariantData");
            methodGetVariantData.setAccessible(true);
            return (BaseVariantData) methodGetVariantData.invoke(variant);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
