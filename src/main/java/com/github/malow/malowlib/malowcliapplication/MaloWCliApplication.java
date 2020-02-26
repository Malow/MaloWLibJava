package com.github.malow.malowlib.malowcliapplication;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.github.malow.malowlib.MaloWLogger;

public abstract class MaloWCliApplication
{
  private static class MappedCommand
  {
    public MappedCommand(String name, Method method, String description)
    {
      this.name = name;
      this.method = method;
      this.description = description;
    }

    public String name;
    public Method method;
    public String description;
  }

  private Scanner in;
  private List<MappedCommand> commands = new ArrayList<>();
  private boolean run = true;

  public MaloWCliApplication()
  {
    this(System.in);
  }

  public MaloWCliApplication(InputStream input)
  {
    MaloWLogger.init();
    this.registerCommands();
    this.in = new Scanner(input);
    this.in.useDelimiter("\n");
  }

  private void registerCommands()
  {
    List<Method> commandMethods = Arrays.asList(this.getClass().getMethods()).stream().filter(m -> m.isAnnotationPresent(Command.class))
        .collect(Collectors.toList());
    for (Method method : commandMethods)
    {
      Parameter[] parametersForMethod = method.getParameters();
      if (parametersForMethod.length > 0)
      {
        if (!parametersForMethod[0].getParameterizedType().equals(String.class))
        {
          throw new RuntimeException("Unsupported parameter type for command-method " + method.getName() + ", only 1 String is supported.");
        }
      }
      this.commands.add(new MappedCommand(method.getName(), method, method.getDeclaredAnnotation(Command.class).description()));
    }
  }

  public void onStart()
  {

  }

  public void run()
  {
    this.onStart();
    while (this.run)
    {
      String input = this.in.next();
      if (input.endsWith("\r"))
      {
        input = input.substring(0, input.length() - 1);
      }
      String command = input;
      String arguments = "";
      int firstSpaceIndex = input.indexOf(" ");
      if (firstSpaceIndex != -1)
      {
        command = input.substring(0, firstSpaceIndex);
        arguments = input.substring(firstSpaceIndex + 1);
      }

      Optional<MappedCommand> mappedCommand = this.getMappedCommandByName(command);
      if (mappedCommand.isPresent())
      {
        try
        {
          long before = System.currentTimeMillis();
          Parameter[] parametersForMethod = mappedCommand.get().method.getParameters();
          if (parametersForMethod.length > 0)
          {
            mappedCommand.get().method.invoke(this, arguments);
          }
          else
          {
            mappedCommand.get().method.invoke(this);
          }
          MaloWLogger.info("Finished command '" + command + "' in " + (System.currentTimeMillis() - before) + "ms.");
        }
        catch (Exception e)
        {
          MaloWLogger.error("Failed to run command: " + command, e);
        }
      }
      else
      {
        System.out.println("Unsupported command: " + command);
      }
    }
    this.onStop();
  }

  private Optional<MappedCommand> getMappedCommandByName(String name)
  {
    return this.commands.stream().filter(c -> c.name.equals(name)).findAny();
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
    System.out.println("Available commands for " + this.getClass().getSimpleName() + ":");
    this.commands.stream().forEach(c -> System.out.println("  " + c.name + " - " + c.description));
  }

  @Command(description = "Closes the application")
  public void exit()
  {
    MaloWLogger.info("Application closed by exit CLI-command");
    this.close();
  }
}
