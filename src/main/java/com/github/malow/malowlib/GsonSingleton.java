package com.github.malow.malowlib;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonSingleton
{
  private static Gson gson = new Gson();
  private static Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

  private GsonSingleton()
  {

  }

  public static String toJson(Object obj)
  {
    try
    {
      return gson.toJson(obj);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to parse object of class " + obj.getClass().getSimpleName() + " into string.", e);
      return null;
    }
  }

  public static String toPrettyJson(Object obj)
  {
    try
    {
      return prettyGson.toJson(obj);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to parse object of class " + obj.getClass().getSimpleName() + " into string.", e);
      return null;
    }
  }

  public static <T> T fromJson(String json, Class<T> targetClass)
  {
    try
    {
      return gson.fromJson(json, targetClass);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to parse String " + json + " into an object of class " + targetClass.getSimpleName(), e);
      return null;
    }
  }

  public static <T> T fromJson(String json, Type type)
  {
    try
    {
      return gson.fromJson(json, type);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to parse String " + json + " into an object of type " + type.getTypeName(), e);
      return null;
    }
  }
}
