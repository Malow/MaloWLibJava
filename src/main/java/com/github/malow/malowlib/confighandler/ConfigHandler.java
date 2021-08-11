package com.github.malow.malowlib.confighandler;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import com.github.malow.malowlib.GsonSingleton;
import com.github.malow.malowlib.MaloWLogger;

/**
 * Usage: Create a class called TestConfig that extends Config. Add implementations of the abstract methods. Have them all return null except getVersion which
 * you set to return "1.0". Add public member variables to TestConfig and set their default values as you want them. Use the static method loadConfig of this
 * class to load it from disk or create it using the defaults if it doesn't exist. The object you get back you can then modify the data of and then call
 * saveConfig on this class to save the changes of it. When you want to make changes to the config-class you rename the old TestConfig to something like
 * TestConfigV1_0, and then create a new class called TestConfig, which you set to return "1.1" in the getVersion. You then set the getNextVersionClass of
 * TestConfigV1_0 to return TestConfig.class, and you set getPreviousVersionClass of TestConfig to return TestConfigV1_0.class. When a TestConfigV1_0 object is
 * read from file using this class's loadConfig it will now automatically iterate back from TestConfig using the getPreviousVersion until it finds one that has
 * the same version as what the file was saved as. It will then copy over any variables that has the same name in both classes, and then call the abstract
 * method upgradeTranslation where you can implement more advanced data translation between the old and new version, casting the parameter "Config oldVersion"
 * to whatever class getPreviousVersion would return to be able to access the variables of it. loadConfig will then iterate forward and continuously upgrade the
 * old configuration until it reaches the latest version, at which point it will overwrite the old config file with this version and return it.
 */

public class ConfigHandler
{
  public static class ConfigException extends Exception
  {
    private static final long serialVersionUID = -5027117305820979723L;

    public ConfigException(String error, Exception cause)
    {
      super(error, cause);
    }

    public ConfigException(String error)
    {
      super(error);
    }
  }

  public static <T extends Config> boolean saveConfig(String filePath, T config) throws ConfigException
  {
    try
    {
      writeConfigToFile(filePath, GsonSingleton.toPrettyJson(config));
    }
    catch (Exception e)
    {
      String errorMsg = "Failed to write config to file \"" + filePath + "\" with object of class " + config.getClass().getSimpleName();
      MaloWLogger.error(errorMsg, e);
      throw new ConfigException(errorMsg, e);
    }
    return true;
  }

  public static <T extends Config> T loadConfig(String filePath, Class<T> configClass) throws ConfigException
  {
    T config = null;
    try
    {
      String fileContents = null;
      try
      {
        fileContents = new String(Files.readAllBytes(Paths.get(filePath)));
        config = handleExistingFile(configClass, fileContents);
      }
      catch (Exception e)
      {
        config = configClass.getDeclaredConstructor().newInstance();
        if (fileContents != null)
        {
          // Save corrupted existing file before overwriting it with a fresh new
          String[] splitFilePath = filePath.split("\\.");
          String corruptedFile = splitFilePath[0] + ".corrupt-" + UUID.randomUUID().toString();
          for (int i = 1; i < splitFilePath.length; i++)
          {
            corruptedFile += "." + splitFilePath[i];
          }
          MaloWLogger.warning("Failed to read config from file \"" + filePath + "\" with class " + configClass.getSimpleName()
              + ". Saved the old corrupted file as \"" + corruptedFile + "\" and created a new config-file with default values.");
          writeConfigToFile(corruptedFile, fileContents);
        }
        else
        {
          MaloWLogger.info("No config file found: " + filePath + ". Creating a new one using default values");
        }
      }
      writeConfigToFile(filePath, GsonSingleton.toPrettyJson(config));
    }
    catch (Exception e)
    {
      String errorMsg = "Failed to load config from file \"" + filePath + "\" with class " + configClass.getSimpleName();
      MaloWLogger.error(errorMsg, e);
      throw new ConfigException(errorMsg, e);
    }
    return config;
  }

  private static <T extends Config> T handleExistingFile(Class<T> configClass, String json) throws Exception
  {
    String currentVersion = GsonSingleton.fromJson(json, configClass).configHeader.get("version");
    Class<T> currentClass = null;
    currentClass = getClassWithVersion(currentVersion, configClass);
    T currentConfig = GsonSingleton.fromJson(json, currentClass);
    T latestConfig = createUpgradedObject(currentConfig);
    return latestConfig;
  }

  @SuppressWarnings("unchecked")
  private static <T extends Config> Class<T> getClassWithVersion(String version, Class<T> configClass) throws Exception
  {
    T current = configClass.getDeclaredConstructor().newInstance();
    while (true)
    {
      if (current.getVersion().equals(version))
      {
        return (Class<T>) current.getClass();
      }
      Class<T> previousClass = (Class<T>) current.getPreviousVersionClass();
      if (previousClass != null)
      {
        current = previousClass.getDeclaredConstructor().newInstance();
      }
      else
      {
        throw new ConfigException("Failed to find a version of " + configClass.getSimpleName() + " with version " + version);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private static <T extends Config> T createUpgradedObject(T current) throws Exception
  {
    while (true)
    {
      Class<T> nextClass = (Class<T>) current.getNextVersionClass();
      if (nextClass != null)
      {
        T upgraded = nextClass.getDeclaredConstructor().newInstance();
        upgraded.upgrade(current);
        current = upgraded;
      }
      else
      {
        return current;
      }
    }
  }

  private static void writeConfigToFile(String filePath, String json) throws Exception
  {
    new File(filePath).getAbsoluteFile().getParentFile().mkdirs();
    Files.write(Paths.get(filePath), json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
  }
}