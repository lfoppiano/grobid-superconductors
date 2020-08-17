package org.grobid.core.engines;

import jep.JepConfig;
import jep.SharedInterpreter;
import org.apache.commons.lang3.SystemUtils;
import org.grobid.core.jni.PythonEnvironmentConfig;
import org.grobid.service.configuration.GrobidSuperconductorsConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.grobid.core.main.LibraryLoader.*;

@Singleton
public class JepEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(JepEngine.class);

    private GrobidSuperconductorsConfiguration configuration;
    private boolean disabled;

    @Inject
    public JepEngine(GrobidSuperconductorsConfiguration configuration) {
        this.configuration = configuration;
        init();
    }

    public void init() {
        File libraryFolder = new File(getLibraryFolder());

        LOGGER.info("Loading JEP native library for the linking module... " + libraryFolder.getAbsolutePath());
        // actual loading will be made at JEP initialization, so we just need to add the path in the
        // java.library.path (JEP will anyway try to load from java.library.path, so explicit file
        // loading here will not help)
        try {
            addLibraryPath(libraryFolder.getAbsolutePath());

            PythonEnvironmentConfig pythonEnvironmentConfig = PythonEnvironmentConfig.getInstanceForVirtualEnv(configuration.getPythonVirtualEnv(), PythonEnvironmentConfig.getActiveVirtualEnv());
            if (pythonEnvironmentConfig.isEmpty()) {
                LOGGER.info("No python environment configured");
            } else {
                LOGGER.info("Configuring python environment: " + pythonEnvironmentConfig.getVirtualEnv());
                LOGGER.info("Adding library paths " + Arrays.toString(pythonEnvironmentConfig.getNativeLibPaths()));
                for (Path path : pythonEnvironmentConfig.getNativeLibPaths()) {
                    if (Files.exists(path)) {
                        addLibraryPath(path.toString());
                    } else {
                        LOGGER.warn(path.toString() + " does not exists. Skipping it. ");
                    }
                }

                if (SystemUtils.IS_OS_MAC) {
                    System.loadLibrary("python" + pythonEnvironmentConfig.getPythonVersion() + "m");
                    System.loadLibrary(DELFT_NATIVE_LIB_NAME);
                } else if (SystemUtils.IS_OS_LINUX) {
                    System.loadLibrary(DELFT_NATIVE_LIB_NAME);
                } else if (SystemUtils.IS_OS_WINDOWS) {
                    throw new UnsupportedOperationException("Linking on Windows is not supported.");
                }
            }

            JepConfig config = new JepConfig();
            config.setRedirectOutputStreams(configuration.isPythonRedirectOutput());
            SharedInterpreter.setConfig(config);
            LOGGER.debug("Configuring JEP to redirect python output.");

        } catch (Exception e) {
            LOGGER.error("Loading JEP native library failed. The linking will be disabled.", e);
            this.disabled = true;
        }
    }

    public boolean getDisabled() {
        return disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }
}
