package com.leekyoungil.illuminati.client.prossor.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.leekyoungil.illuminati.client.prossor.config.IlluminatiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 15/07/2017.
 */
public class FileUtils {

    private final static Logger FILEUTIL_LOGGER = LoggerFactory.getLogger(FileUtils.class);
    private final static List<String> CONFIG_FILE_EXTENSTIONS = Arrays.asList(new String[]{"properties", "yml", "yaml"});
    private final static String PROFILES_PHASE = System.getProperty("spring.profiles.active");

    public static String getPropertiesValueByKey (Messager messager, final String configPropertiesFileName, final String key) {
        final IlluminatiProperties illuminatiProperties = getIlluminatiProperties(messager, configPropertiesFileName);
        String propertiesValue = null;

        if (StringUtils.isValid(key) && illuminatiProperties != null) {
            try {
                final String methodName = "get" + key.substring(0, 1).toUpperCase() + key.substring(1);
                final Method getNameMethod = IlluminatiProperties.class.getMethod(methodName);
                propertiesValue = (String) getNameMethod.invoke(illuminatiProperties);
            } catch (NoSuchMethodException ex) {
                FileUtils.FILEUTIL_LOGGER.debug("Sorry, unable to find method. ("+ex.toString()+")");
            }catch (IllegalAccessException ex) {
                FileUtils.FILEUTIL_LOGGER.debug("Sorry, unable to access method. ("+ex.toString()+")");
            } catch (InvocationTargetException ex) {
                FileUtils.FILEUTIL_LOGGER.debug("Sorry, unable to package method. ("+ex.toString()+")");
            }
        }

        return propertiesValue;
    }

    public static IlluminatiProperties getIlluminatiProperties (Messager messager, final String configPropertiesFileName) {
        IlluminatiProperties illuminatiProperties = null;

        for (String extension : CONFIG_FILE_EXTENSTIONS) {
            String dotBeforeExtension = ".";

            if (StringUtils.isValid(PROFILES_PHASE)) {
                dotBeforeExtension = "-" + PROFILES_PHASE + ".";
            }

            final String fullFileName = configPropertiesFileName + dotBeforeExtension + extension;
            illuminatiProperties = getIlluminatiPropertiesByFile(messager, fullFileName);

            if (illuminatiProperties != null) {
                break;
            }
        }

        if (illuminatiProperties == null) {
            final String fullFileName = "application.properties";
            illuminatiProperties = getIlluminatiPropertiesByFile(messager, fullFileName);
        }

        if (illuminatiProperties == null) {
            if (messager != null) {
                messager.printMessage(Diagnostic.Kind.ERROR,"Sorry, unable to find " + configPropertiesFileName);
            }

            FileUtils.FILEUTIL_LOGGER.debug("Sorry, unable to find " + configPropertiesFileName);
        }

        return illuminatiProperties;
    }

    private static IlluminatiProperties getIlluminatiPropertiesByFile (Messager messager, final String configPropertiesFileName) {
        final InputStream input = FileUtils.class.getClassLoader().getResourceAsStream(configPropertiesFileName);
        IlluminatiProperties illuminatiProperties = null;

        if(input == null){
            return null;
        }

        try {
            if (configPropertiesFileName.indexOf(".yml") > -1 || configPropertiesFileName.indexOf(".yaml") > -1) {
                ObjectMapper mapper  = new ObjectMapper(new YAMLFactory());
                mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
                illuminatiProperties = mapper.readValue(input, IlluminatiProperties.class);
            } else {
                final Properties prop = new Properties();
                prop.load(input);

                if (prop == null) {
                    if (messager != null) {
                        messager.printMessage(Diagnostic.Kind.ERROR,"Sorry, unable to convert properties file to Properties. ("+configPropertiesFileName+")");
                    }

                    FileUtils.FILEUTIL_LOGGER.debug("Sorry, unable to convert properties file to Properties. ("+configPropertiesFileName+")");

                    return null;
                }

                illuminatiProperties = new IlluminatiProperties(prop);
            }
        } catch (IOException ex) {
            if (messager != null) {
                messager.printMessage(Diagnostic.Kind.WARNING,"Sorry, something is wrong in read process. ("+ex.toString()+")");
            }

            FileUtils.FILEUTIL_LOGGER.debug("Sorry, something is wrong in read process. ("+ex.toString()+")");
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException ex) {
                    if (messager != null) {
                        messager.printMessage(Diagnostic.Kind.WARNING,"Sorry, something is wrong in close InputStream process. ("+ex.toString()+")");
                    }

                    FileUtils.FILEUTIL_LOGGER.debug("Sorry, something is wrong in close InputStream process. ("+ex.toString()+")");
                }
            }
        }

        return illuminatiProperties;
    }
}
