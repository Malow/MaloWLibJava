package com.github.malow.malowlib.malowcliapplication;

import java.io.InputStream;
import java.lang.reflect.Method;
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
  }

  private void registerCommands()
  {
    List<Method> commandMethods = Arrays.asList(this.getClass().getMethods()).stream().filter(m -> m.isAnnotationPresent(Command.class))
        .collect(Collectors.toList());
    for (Method method : commandMethods)
    {
      this.commands.add(new MappedCommand(method.getName().toLowerCase(), method, method.getDeclaredAnnotation(Command.class).description()));
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
      String input = this.in.next().toLowerCase();
      Optional<MappedCommand> mappedComamnd = this.getMappedCommandByName(input);
      if (mappedComamnd.isPresent())
      {
        try
        {
          long before = System.currentTimeMillis();
          mappedComamnd.get().method.invoke(this);
          MaloWLogger.info("Finished command " + input + " in " + (System.currentTimeMillis() - before) + "ms.");
        }
        catch (Exception e)
        {
          MaloWLogger.error("Failed to run command: " + input, e);
        }
      }
      else
      {
        System.out.println("Unsupported command: " + input);
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
