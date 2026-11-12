package com.winlator.core;

import android.content.Context;

import app.gamenative.PrefManager;

import com.winlator.core.envvars.EnvVars;
import com.winlator.xenvironment.ImageFs;

import java.io.File;

public class DXVKHelper {
    public static final String DEFAULT_CONFIG = "version="+DefaultVersion.DXVK+",framerate=0,maxDeviceMemory=0";

    public static KeyValueSet parseConfig(Object config) {
        String data = config != null && !config.toString().isEmpty() ? config.toString() : DEFAULT_CONFIG;
        return new KeyValueSet(data);
    }

    /**
     * The value of DXVK_CONFIG. DXVK splits it on ';' and takes it verbatim: the env var reaches the process through
     * execve, so there is no shell to strip quotes. Wrapping it in quotes or separating options with newlines makes
     * DXVK ignore every option.
     */
    public static String buildConfigString(KeyValueSet config) {
        java.util.ArrayList<String> options = new java.util.ArrayList<>();

        String maxDeviceMemory = config.get("maxDeviceMemory");
        if (!maxDeviceMemory.isEmpty() && !maxDeviceMemory.equals("0")) {
            options.add("dxgi.maxDeviceMemory = "+maxDeviceMemory);
            options.add("dxgi.maxSharedMemory = "+maxDeviceMemory);
        }

        String numCompilerThreads = config.get("numCompilerThreads");
        if (!numCompilerThreads.isEmpty() && !numCompilerThreads.equals("0")) {
            options.add("dxvk.numCompilerThreads = "+numCompilerThreads);
        }

        String maxFeatureLevel = config.get("maxFeatureLevel");
        if (!maxFeatureLevel.isEmpty() && !maxFeatureLevel.equals("0")) {
            options.add("d3d11.maxFeatureLevel = "+maxFeatureLevel);
        }

        String customDevice = config.get("customDevice");
        if (customDevice.contains(":")) {
            String[] parts = customDevice.split(":");
            options.add("dxgi.customDeviceId = "+parts[0]);
            options.add("dxgi.customVendorId = "+parts[1]);
            options.add("d3d9.customDeviceId = "+parts[0]);
            options.add("d3d9.customVendorId = "+parts[1]);
            options.add("dxgi.customDeviceDesc = \""+parts[2]+"\"");
            options.add("d3d9.customDeviceDesc = \""+parts[2]+"\"");
        }
        if (config.getBoolean("constantBufferRangeCheck")) {
            options.add("d3d11.constantBufferRangeCheck = True");
        }
        return String.join(";", options);
    }

    public static void setEnvVars(Context context, KeyValueSet config, EnvVars envVars) {
        ImageFs imageFs = ImageFs.find(context);
        envVars.put("DXVK_STATE_CACHE_PATH", imageFs.getRootDir().getAbsolutePath() + ImageFs.CACHE_PATH);
        // DXVK writes <exe>_d3d11.log / _dxgi.log next to the game executable; keep it quiet unless diagnostics are on.
        envVars.put("DXVK_LOG_LEVEL", PrefManager.INSTANCE.getEnableWineDebug() ? "info" : "none");

        File rootDir = ImageFs.find(context).getRootDir();
        String maxFeatureLevel = config.get("maxFeatureLevel");
        if (!maxFeatureLevel.isEmpty() && !maxFeatureLevel.equals("0")) {
            envVars.put("DXVK_FEATURE_LEVEL", maxFeatureLevel);
        }

        String framerate = config.get("framerate");
        if (!framerate.isEmpty() && !framerate.equals("0")) {
            envVars.put("DXVK_FRAME_RATE", framerate);
        }

        String async = config.get("async");
        if (!async.isEmpty() && !async.equals("0"))
            envVars.put("DXVK_ASYNC", "1");

        String asyncCache = config.get("asyncCache");
        if (!asyncCache.isEmpty() && !asyncCache.equals("0"))
            envVars.put("DXVK_GPLASYNCCACHE", "1");

        String content = buildConfigString(config);

        envVars.put("DXVK_CONFIG_FILE", rootDir + ImageFs.CONFIG_PATH+"/dxvk.conf");
        envVars.put("DXVK_CONFIG", content);
    }

    public static void setVKD3DEnvVars(Context context, KeyValueSet config, EnvVars envVars) {
        String featureLevel = config.get("vkd3dFeatureLevel", "12_1");
        envVars.put("VKD3D_FEATURE_LEVEL", featureLevel);
    }
}
