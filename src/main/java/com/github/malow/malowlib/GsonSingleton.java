package com.github.malow.malowlib;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GsonSingleton
{
  private static final DateTimeFormatter LOCALDATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;//.ofPattern("yyyy-MM-dd'T'HH:mm'Z'");

  private static Gson gson = new GsonBuilder()
      .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
      .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
      .create();

  private static Gson prettyGson = new GsonBuilder()
      .setPrettyPrinting()
      .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
      .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
      .create();

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

  // Serializers/Deserializers
  private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime>
  {
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context)
    {
      return new JsonPrimitive(LOCALDATETIME_FORMATTER.format(src));
    }
  }

  private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime>
  {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
      return LocalDateTime.parse(json.getAsString(), LOCALDATETIME_FORMATTER);
    }
  }
}
