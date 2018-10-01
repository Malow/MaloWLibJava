package com.github.malow.malowlib;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MaloWUtils
{
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getGenericClassFor(Object o) throws ClassNotFoundException
  {
    Type genericSuperClass = o.getClass().getGenericSuperclass();
    Type type = ((ParameterizedType) genericSuperClass).getActualTypeArguments()[0];
    return (Class<T>) Class.forName(type.getTypeName());
  }
}
