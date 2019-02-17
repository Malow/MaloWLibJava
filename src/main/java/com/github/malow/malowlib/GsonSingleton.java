package com.github.malow.malowlib;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private static Gson gson = createGsonBuilder()
      .create();

  private static Gson prettyGson = createGsonBuilder()
      .setPrettyPrinting()
      .create();

  private static GsonBuilder createGsonBuilder()
  {
    return new GsonBuilder()
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer())
        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer())
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer());
  }

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

  public static <T> List<T> fromJsonAsList(String json, Class<T[]> targetClass)
  {
    if (json == null || json.equals(""))
    {
      return new ArrayList<>();
    }
    return Arrays.asList(fromJson(json, targetClass));
  }

  // Serializers/Deserializers
  private static class LocalDateTimeSerializer implements JsonSerializer<LocalDateTime>
  {
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context)
    {
      return new JsonPrimitive(LOCAL_DATE_TIME_FORMATTER.format(src));
    }
  }

  private static class LocalDateTimeDeserializer implements JsonDeserializer<LocalDateTime>
  {

    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
      return LocalDateTime.parse(json.getAsString(), LOCAL_DATE_TIME_FORMATTER);
    }
  }

  private static class ZonedDateTimeSerializer implements JsonSerializer<ZonedDateTime>
  {
    @Override
    public JsonElement serialize(ZonedDateTime src, Type typeOfSrc, JsonSerializationContext context)
    {
      return new JsonPrimitive(DATETIME_FORMATTER.format(src));
    }
  }

  private static class ZonedDateTimeDeserializer implements JsonDeserializer<ZonedDateTime>
  {

    @Override
    public ZonedDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
      return ZonedDateTime.parse(json.getAsString(), DATETIME_FORMATTER);
    }
  }
}
