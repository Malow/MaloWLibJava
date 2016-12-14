package com.github.malow.malowlib;

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
    return gson.toJson(obj);
  }

  public static String toPrettyJson(Object obj)
  {
    return prettyGson.toJson(obj);
  }

  public static <T> T fromJson(String json, Class<T> targetClass)
  {
    return gson.fromJson(json, targetClass);
  }
}
