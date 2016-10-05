package com.github.malow.malowlib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MaloWLogger
{

  private static final String FOLDER = "logs/";
  private static final String ALL_FILE = "all.log";

  public enum LogLevel
  {
    INFO("INFO", "info.log"), //
    WARNING("WARNING", "warning.log"), //
    ERROR("ERROR", "error.log");

    public final String name;
    public final String fileName;

    private LogLevel(String name, String fileName)
    {
      this.name = name;
      this.fileName = fileName;
    }
  }

  static
  {
    logToFile(ALL_FILE, "\nApplication Launch");
    logToFile(LogLevel.INFO.fileName, "\nApplication Launch");
    logToFile(LogLevel.WARNING.fileName, "\nApplication Launch");
    logToFile(LogLevel.ERROR.fileName, "\nApplication Launch");
  }

  public static void log(LogLevel level, String msg)
  {
    logToFile(ALL_FILE, level.name + ": " + msg);
    logToFile(level.fileName, msg);
  }

  public static void logToFile(String fileName, String msg)
  {
    msg += "\n";
    try
    {
      new File(FOLDER + fileName).getParentFile().mkdirs();
      Files.write(Paths.get(FOLDER + fileName), msg.getBytes(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }
    catch (IOException e)
    {
      System.out.println(e.toString());
    }
  }
}