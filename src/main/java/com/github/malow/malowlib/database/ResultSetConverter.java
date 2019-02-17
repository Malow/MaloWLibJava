package com.github.malow.malowlib.database;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class ResultSetConverter
{
  public static final DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
      .optionalStart()
      .appendPattern(".SSSSSSSSS")
      .optionalEnd()
      .optionalStart()
      .appendPattern(".SSSSSS")
      .optionalEnd()
      .toFormatter();

  public static Object getValueFromResultSetForField(Field field, Class<?> fieldClass, ResultSet resultSet) throws Exception
  {
    if (fieldClass.equals(String.class))
    {
      return resultSet.getString(field.getName());
    }
    else if (fieldClass.equals(Integer.class))
    {
      return resultSet.getInt(field.getName());
    }
    else if (fieldClass.equals(Double.class))
    {
      return resultSet.getDouble(field.getName());
    }
    else if (fieldClass.equals(LocalDateTime.class))
    {
      String timestamp = resultSet.getString(field.getName());
      if (timestamp != null)
      {
        return LocalDateTime.parse(timestamp, dateFormatter);
      }
      return null;
    }
    else
    {
      throw new Exception("Type not supported in ResultSetConverter: " + fieldClass);
    }
  }
}
