package com.github.malow.malowlib.database;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ResultSetConverter
{
  public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss][.SSS]");

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
      throw new Exception("Type not supported: " + fieldClass);
    }
  }
}
