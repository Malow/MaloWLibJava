package com.github.malow.malowlib.malowcliapplication;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.malow.malowlib.MaloWLogger;

public abstract class MaloWCliApplication
{
  private Scanner in;
  private boolean run = true;

  public MaloWCliApplication()
  {
    this(System.in);
  }

  public MaloWCliApplication(InputStream input)
  {
    MaloWLogger.init();
    this.validateCommands();
    this.in = new Scanner(input);
    this.in.useDelimiter("\n");
  }

  private List<Method> getCommandMethods()
  {
    return Arrays.asList(this.getClass().getMethods()).stream().filter(m -> m.isAnnotationPresent(Command.class)).collect(Collectors.toList());
  }

  private void validateCommands()
  {
    List<Method> commandMethods = this.getCommandMethods();
    Set<String> seenCommandMethodNames = new HashSet<>();
    for (Method method : commandMethods)
    {
      if (!seenCommandMethodNames.add(method.getName()))
      {
        throw new RuntimeException("Duplicate commands for " + method.getName());
      }

      java.lang.reflect.Parameter[] parameters = method.getParameters();
      Set<String> seenParameterFlags = new HashSet<>();
      for (java.lang.reflect.Parameter parameter : parameters)
      {
        Parameter parameterAnnotation = parameter.getAnnotation(Parameter.class);
        if (parameterAnnotation == null)
        {
          throw new RuntimeException("Missing @Parameter annotation for a parameter for command-method '" + method.getName() + "'.");
        }

        if (!seenParameterFlags.add(parameterAnnotation.flag()))
        {
          throw new RuntimeException("Duplicate parameter flags for command '" + method.getName() + "' " + parameterAnnotation.flag());
        }

        Type parameterType = parameter.getParameterizedType();
        if (!parameterType.equals(String.class) && !parameterType.equals(Integer.class)
            && !parameterType.equals(Double.class) && !parameterType.equals(Boolean.class))
        {
          throw new RuntimeException("Unsupported parameter type '" + parameterType.getTypeName() + "' for flag '" + parameterAnnotation.flag()
              + "' for command-method '" + method.getName() + "', only Strings, Integers, Doubles and Booleans are supported.");
        }

        if (parameterType.equals(Boolean.class) && parameterAnnotation.optional())
        {
          throw new RuntimeException("Unnecessary optional property on flag '" + parameterAnnotation.flag() + "' for command-method '"
              + method.getName() + "', Booleans are always optional by default (flag present = true, not present = null).");
        }
      }
    }
  }

  public void run()
  {
    this.onStart();
    while (this.run)
    {
      String input = this.in.next();
      this.handleInput(input);
    }
    this.onStop();
  }

  private void handleInput(String input)
  {
    if (input.isEmpty())
    {
      return;
    }
    if (input.endsWith("\r"))
    {
      input = input.substring(0, input.length() - 1);
    }
    String command = "";
    String arguments = "";
    int firstSpaceIndex = input.indexOf(" ");
    if (firstSpaceIndex != -1)
    {
      command = input.substring(0, firstSpaceIndex);
      arguments = input.substring(firstSpaceIndex);
    }
    else
    {
      command = input;
    }
    for (Method commandMethod : this.getCommandMethods())
    {
      if (commandMethod.getName().equals(command))
      {
        this.handleCommand(commandMethod, arguments);
        return;
      }
    }
    System.out.println("Unsupported command: '" + command + "'. Type 'help' to see available commands.");
  }

  private static class StringExtractionResults
  {
    public String value;
    public String remainingString;
  }

  private Optional<StringExtractionResults> extractStringFromStart(String regex, String str)
  {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(str);
    if (!matcher.find())
    {
      return Optional.empty();
    }
    StringExtractionResults results = new StringExtractionResults();
    results.value = matcher.group(1) != null ? matcher.group(1) : matcher.group();
    results.remainingString = str.substring(matcher.end());
    return Optional.of(results);
  }

  private void handleCommand(Method commandMethod, String arguments)
  {
    Map<String, String> flagValues = new HashMap<>();
    String currentFlag = null;
    while (true)
    {
      if (currentFlag != null)
      {
        String value = "";
        Optional<StringExtractionResults> results = this.extractStringFromStart("^\"(.*?)\"|^([^ ]*)", arguments);
        if (results.isPresent())
        {
          value = results.get().value;
          arguments = results.get().remainingString;
        }
        flagValues.put(currentFlag, value);
        currentFlag = null;
      }
      else
      {
        Optional<StringExtractionResults> results = this.extractStringFromStart("^ ?(-[a-zA-Z0-9]{1,}) ?", arguments);
        if (results.isPresent())
        {
          currentFlag = results.get().value;
          arguments = results.get().remainingString;
        }
        else
        {
          break;
        }
      }
    }
    if (!arguments.isBlank())
    {
      System.out.println("Bad parameter format for '" + commandMethod.getName() + "': '" + arguments
          + "'. Type 'help' to see available commands.");
      return;
    }

    java.lang.reflect.Parameter[] parameters = commandMethod.getParameters();
    for (Map.Entry<String, String> entry : flagValues.entrySet())
    {
      if (Arrays.stream(parameters).map(param -> param.getAnnotation(Parameter.class)).noneMatch(param -> param.flag().equals(entry.getKey())))
      {
        System.out.println("Unrecognized flag '" + entry.getKey() + "' for command '" + commandMethod.getName()
            + "'. Type 'help' to see available commands.");
        return;
      }
    }

    List<Object> invokeArguments = new ArrayList<>();
    for (java.lang.reflect.Parameter parameter : parameters)
    {
      Parameter parameterAnnotation = parameter.getAnnotation(Parameter.class);
      String stringValue = flagValues.get(parameterAnnotation.flag());
      if (stringValue == null)
      {
        if (!parameterAnnotation.optional() && !parameter.getType().equals(Boolean.class))
        {
          System.out.println("Missing mandatory parameter '" + parameterAnnotation.flag() + "' for command '" + commandMethod.getName() + "'.");
          return;
        }
        invokeArguments.add(null);
        continue;
      }
      Object value = stringValue;
      try
      {
        if (parameter.getType() == Integer.class)
        {
          value = Integer.parseInt(stringValue);
        }
        if (parameter.getType() == Double.class)
        {
          value = Double.parseDouble(stringValue);
        }
        if (parameter.getType() == Boolean.class)
        {
          if (stringValue.isEmpty())
          {
            value = true;
          }
          else
          {
            System.out.println("Unexpected value for flag '" + parameterAnnotation.flag() + "' for command '"
                + commandMethod.getName() + "', expected no value for a boolean parameter.");
            return;
          }
        }
      }
      catch (Exception e)
      {
        System.out.println("Failed to parse the value for parameter '" + parameterAnnotation.flag() + "' for command '"
            + commandMethod.getName() + "' into a number.");
        return;
      }
      invokeArguments.add(value);
    }

    long before = System.currentTimeMillis();
    try
    {
      commandMethod.invoke(this, invokeArguments.toArray());
      MaloWLogger.info("Finished command '" + commandMethod.getName() + "' in " + (System.currentTimeMillis() - before) + "ms.");
    }
    catch (Exception e)
    {
      MaloWLogger.error("Error while running command '" + commandMethod.getName() + "' after " + (System.currentTimeMillis() - before) + "ms.", e);
    }
  }

  public void onStart()
  {

  }

  public void onStop()
  {

  }

  public void close()
  {
    this.run = false;
    this.in.close();
  }

  @Command(description = "Lists all available commands")
  public void help()
  {
    System.out.println("Available commands for '" + this.getClass().getSimpleName() + "':");
    for (Method commandMethod : this.getCommandMethods())
    {
      Command commandAnnotation = commandMethod.getAnnotation(Command.class);
      String s = "   " + commandMethod.getName();
      for (java.lang.reflect.Parameter parameter : commandMethod.getParameters())
      {
        Parameter parameterAnnotation = parameter.getAnnotation(Parameter.class);
        s += " " + parameterAnnotation.flag() + " <" + parameter.getType().getSimpleName() + ", " + parameterAnnotation.description();
        if (parameterAnnotation.optional() || parameter.getType().equals(Boolean.class))
        {
          s += ", optional>";
        }
        else
        {
          s += ">";
        }
      }
      System.out.println(s + "\n      " + commandAnnotation.description());
    }
  }

  @Command(description = "Closes the application")
  public void exit()
  {
    MaloWLogger.info("Application closed by exit CLI-command");
    this.close();
  }
}
