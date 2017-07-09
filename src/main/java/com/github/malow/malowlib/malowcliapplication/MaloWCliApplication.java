package com.github.malow.malowlib.malowcliapplication;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

import com.github.malow.malowlib.MaloWLogger;

public abstract class MaloWCliApplication
{
  Scanner in = new Scanner(System.in);
  private Map<String, Method> commands = new HashMap<>();
  private boolean run = true;

  public MaloWCliApplication()
  {
    MaloWLogger.init();
    this.registerCommands();
  }

  private void registerCommands()
  {
    List<Method> commandMethods = Arrays.asList(this.getClass().getMethods()).stream().filter(m -> m.isAnnotationPresent(Command.class))
        .collect(Collectors.toList());
    for (Method method : commandMethods)
    {
      this.commands.put(method.getName().toLowerCase(), method);
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
      Method method = this.commands.get(input);
      if (method != null)
      {
        try
        {
          long before = System.currentTimeMillis();
          method.invoke(this);
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

  public void onStop()
  {

  }

  public void close()
  {
    this.run = false;
    this.in.close();
  }

  @Command
  public void exit()
  {
    MaloWLogger.info("Application closed by exit CLI-command");
    this.close();
  }
}
