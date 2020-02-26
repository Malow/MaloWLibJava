package com.github.malow.malowlib;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithParameter;

public class PersistedData
{
  public static volatile Map<String, DataContainer<?>> dataFiles = new HashMap<>();

  public static class DataContainer<T>
  {
    public volatile T data;
    public volatile int useCount = 0;

    public DataContainer(Class<T> dataClass) throws Exception
    {
      this.data = dataClass.getDeclaredConstructor().newInstance();
    }

    public DataContainer(T data) throws Exception
    {
      this.data = data;
    }
  }

  /**
   * @param dataClass
   *          The class of the data you want to be working with in the Lambda-function
   * @param file
   *          The file-path you want your data to be backed by
   * @param f
   *          The Lambda-function where the data is loaded, used however you want, and saved afterwards.
   * @return The data-object. Do note that further changes to this object outside of the Lambda-function will probably not be persisted to the file.
   * @throws Exception
   */
  public static <T> T useData(Class<T> dataClass, String file, CheckedFunctionWithParameter<T> f) throws Exception
  {
    T data = getOrLoadData(file, dataClass);
    try
    {
      f.apply(data);
      return data;
    }
    catch (Exception e)
    {
      MaloWLogger.error(PersistedData.class.getSimpleName() + " recevied Exception while executing lambda-function.", e);
      throw e;
    }
    finally
    {
      decreaseOrSaveData(file);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T getOrLoadData(String file, Class<T> dataClass) throws Exception
  {
    synchronized (PersistedData.class)
    {
      DataContainer<T> dataContainer = null;
      if (dataFiles.containsKey(file))
      {
        try
        {
          dataContainer = (DataContainer<T>) dataFiles.get(file);
          dataContainer.useCount++;
        }
        catch (Exception e)
        {
          MaloWLogger.error("Error, tried to use data-file " + file + " which is already used as class "
              + dataFiles.get(file).getClass().getSimpleName() + " with a different class");
          throw e;
        }
      }
      else
      {
        if (Files.exists(Paths.get(file)))
        {
          dataContainer = new DataContainer<>(
              GsonSingleton.fromJson(Files.readAllLines(Paths.get(file)).stream().collect(Collectors.joining()), dataClass));
        }
        else
        {
          dataContainer = new DataContainer<>(dataClass);
        }
        dataFiles.put(file, dataContainer);
      }
      dataContainer.useCount++;
      return dataContainer.data;
    }
  }

  private static void decreaseOrSaveData(String file) throws Exception
  {
    synchronized (PersistedData.class)
    {
      if (!dataFiles.containsKey(file))
      {
        throw new Exception("Tried to decrease and save file " + file + " but had no data in memory for that file.");
      }
      DataContainer<?> dataContainer = dataFiles.get(file);
      dataContainer.useCount--;
      if (dataContainer.useCount < 1)
      {
        dataFiles.remove(file);
        String json = GsonSingleton.toJson(dataContainer.data);
        Files.write(Paths.get(file), json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
      }
    }
  }
}
