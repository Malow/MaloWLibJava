package com.github.malow.malowlib;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MaloWLogger
{
  private static final String FOLDER = "logs/";
  private static final String ALL_FILE = "all.log";
  private static LogLevel threshold = LogLevel.INFO;
  private static boolean logToFile = true;
  private static boolean logToSyso = true;
  private static boolean logToSpecificFiles = false;

  public enum LogLevel
  {
    INFO("INFO", "info.log", 0), WARNING("WARNING", "warning.log", 1), ERROR("ERROR", "error.log", 2), NONE("NONE", "", 1000);

    public final String name;
    public final String fileName;
    public final int level;

    private LogLevel(String name, String fileName, int level)
    {
      this.name = name;
      this.fileName = fileName;
      this.level = level;
    }
  }

  static
  {
    writeToFile(ALL_FILE, "\n-----------------------------------\n");
    writeToFile(LogLevel.INFO.fileName, "\n-----------------------------------\n");
    writeToFile(LogLevel.WARNING.fileName, "\n-----------------------------------\n");
    writeToFile(LogLevel.ERROR.fileName, "\n-----------------------------------\n");
    logSpecificLevel(ALL_FILE, "Application Launch");
    logSpecificLevel(LogLevel.INFO.fileName, "Application Launch");
    logSpecificLevel(LogLevel.WARNING.fileName, "Application Launch");
    logSpecificLevel(LogLevel.ERROR.fileName, "Application Launch");
  }

  public static void setLoggingThreshold(LogLevel level)
  {
    threshold = level;
  }

  public static void setLogToFile(boolean val)
  {
    logToFile = val;
  }

  public static void setLogToSyso(boolean val)
  {
    logToSyso = val;
  }
  
  public static void setLogToSpecificFiles(boolean val)
  {
    logToSpecificFiles = val;
  }

  public static void info(String msg)
  {
    if (threshold.level <= LogLevel.INFO.level)
    {
      LogLevel level = LogLevel.INFO;
      log(level, msg);
    }
  }

  public static void warning(String msg)
  {
    if (threshold.level <= LogLevel.WARNING.level)
    {
      LogLevel level = LogLevel.WARNING;
      log(level, msg);
    }
  }

  public static void error(String msg, Exception e)
  {
    if (threshold.level <= LogLevel.ERROR.level)
    {
      LogLevel level = LogLevel.ERROR;
      msg = msg + "\n    " + e.getClass().getName() + ": " + e.getMessage();
      StackTraceElement[] stack = e.getStackTrace();
      for (StackTraceElement element : stack)
      {
        msg += "\n    " + element.toString();
      }
      log(level, msg);
    }
  }

  private static void log(LogLevel level, String msg)
  {
    logSpecificLevel(ALL_FILE, level.name + ": " + msg);
    if (logToSpecificFiles)
    {
      logSpecificLevel(level.fileName, msg);
    }
  }

  private static void logSpecificLevel(String fileName, String msg)
  {
    msg = getCurrentDateTime() + " - " + msg;
    if (logToSyso && fileName.equals(ALL_FILE))
    {
      System.out.println(msg);
    }
    if (logToFile)
    {
      msg += "\n";
      writeToFile(fileName, msg);
    }
  }

  private static void writeToFile(String fileName, String msg)
  {
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

  private static String getCurrentDateTime()
  {
    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    Date date = new Date();
    return dateFormat.format(date);
  }
}
