package com.github.malow.malowlib;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class MaloWUtils
{
  @SuppressWarnings("unchecked")
  public static <T> Class<T> getGenericClassForParent(Object o) throws ClassNotFoundException
  {
    Type genericSuperClass = o.getClass().getGenericSuperclass();
    Type type = ((ParameterizedType) genericSuperClass).getActualTypeArguments()[0];
    return (Class<T>) Class.forName(type.getTypeName());
  }

  @SuppressWarnings("unchecked")
  @Deprecated /* This is pretty damn haxxy yo */
  public static <T> Class<T> getGenericClassForField(Field f) throws Exception
  {
    Type type = ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0];
    if (type instanceof Class<?>)
    {
      return (Class<T>) type;
    }
    Field f2 = type.getClass().getDeclaredField("actualTypeArguments");
    f2.setAccessible(true);
    type = ((Type[]) f2.get(type))[0];
    return (Class<T>) type;
  }
}
