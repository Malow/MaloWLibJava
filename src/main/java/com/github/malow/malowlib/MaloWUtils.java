package com.github.malow.malowlib;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import com.github.malow.malowlib.lambdainterfaces.CheckedFunction;
import com.github.malow.malowlib.lambdainterfaces.CheckedFunctionWithReturn;

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

  public static void ignoreException(CheckedFunction f)
  {
    try
    {
      f.apply();
    }
    catch (Exception e)
    {
    }
  }

  public static void logException(String msg, CheckedFunction f)
  {
    try
    {
      f.apply();
    }
    catch (Exception e)
    {
      MaloWLogger.error(msg, e);
    }
  }

  public static <T> T logException(String msg, CheckedFunctionWithReturn<T> f)
  {
    try
    {
      return f.apply();
    }
    catch (Exception e)
    {
      MaloWLogger.error(msg, e);
    }
    return null;
  }

  public static Object callNonVisibleMethod(Object obj, String methodName)
  {
    return logException("Error when calling callNonVisible: ", () ->
    {
      Method method = obj.getClass().getDeclaredMethod(methodName);
      method.setAccessible(true);
      return method.invoke(obj);
    });
  }

  public static <T> T callNonVisibleConstructor(Class<T> clazz, List<Pair<Class<?>, Object>> parameters)
  {
    return logException("Error when calling callNonVisible: ", () ->
    {
      List<Class<?>> parmaterClassesList = parameters.stream().map(p -> p.first).collect(Collectors.toList());
      Class<?>[] parameterClassesArray = new Class<?>[parmaterClassesList.size()];
      parmaterClassesList.toArray(parameterClassesArray);
      Constructor<T> con = clazz.getDeclaredConstructor(parameterClassesArray);
      con.setAccessible(true);

      return con.newInstance(parameters.stream().map(p -> p.second).toArray());
    });
  }

  public static Set<Class<?>> getAllClassesInPackage(String packageName)
  {
    Reflections ref = new Reflections(packageName, new SubTypesScanner(false));
    return ref.getSubTypesOf(Object.class);
  }

  public static <T> Set<Class<? extends T>> getAllClassesExtending(Class<T> superClass)
  {
    Reflections ref = new Reflections("", new SubTypesScanner(false));
    return ref.getSubTypesOf(superClass);
  }
}
