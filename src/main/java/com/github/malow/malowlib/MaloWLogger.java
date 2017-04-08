package com.github.malow.malowlib;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MaloWLogger
{

  private static final String FOLDER = "logs/";
  private static LogLevel threshold = LogLevel.INFO;
  private static boolean enabled = true;
  private static boolean logToFile = true;
  private static boolean logToSyso = true;
  private static boolean logToSpecificFiles = true;

  private enum LogLevel
  {
    ALL("ALL", "all.log", -1),
    INFO("INFO", "info.log", 0),
    WARNING("WARNING", "warning.log", 1),
    ERROR("ERROR", "error.log", 2);

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
    writeToFile(LogLevel.ALL.fileName, "\n-----------------------------------\n");
    writeToFile(LogLevel.INFO.fileName, "\n-----------------------------------\n");
    writeToFile(LogLevel.WARNING.fileName, "\n-----------------------------------\n");
    writeToFile(LogLevel.ERROR.fileName, "\n-----------------------------------\n");
    logForAllLevels("MaloWLogger: Application Launch", true);
  }

  public static void disableLogging()
  {
    enabled = false;
    logForAllLevels("MaloWLogger: Logging disabled.", true);
  }

  public static void enableLogging()
  {
    enabled = true;
    logForAllLevels("MaloWLogger: Logging re-enabled.", true);
  }

  public static void setLoggingThresholdToInfo()
  {
    setLoggingThreshold(LogLevel.INFO);
  }

  public static void setLoggingThresholdToWarning()
  {
    setLoggingThreshold(LogLevel.WARNING);
  }

  public static void setLoggingThresholdToError()
  {
    setLoggingThreshold(LogLevel.ERROR);
  }

  private static void setLoggingThreshold(LogLevel level)
  {
    threshold = level;
    logForAllLevels("MaloWLogger: Logging Threshold set to " + threshold.name + ". ", true);
  }

  public static void disableLoggingToFiles()
  {
    logToFile = false;
    logForAllLevels("MaloWLogger: Logging to files disabled.", true);
  }

  public static void enableLoggingToFiles()
  {
    logToFile = true;
    logForAllLevels("MaloWLogger: Logging to files re-enabled.", true);
  }

  public static void disableLoggingToSyso()
  {
    logToSyso = false;
    System.out.println(getCurrentDateTime() + " - " + "MaloWLogger: Logging to system-out disabled.");
  }

  public static void enableLoggingToSyso()
  {
    logToSyso = true;
    System.out.println(getCurrentDateTime() + " - " + "MaloWLogger: Logging to system-out re-enabled.");
  }

  public static void disableLoggingToSpecificFiles()
  {
    logToSpecificFiles = false;
    String msg = "MaloWLogger: Logging to specific files for different log levels disabled, see " + LogLevel.ALL.fileName
        + " for the combined output of every level.";
    logForSpecificLevel(LogLevel.INFO, msg, true);
    logForSpecificLevel(LogLevel.WARNING, msg, true);
    logForSpecificLevel(LogLevel.ERROR, msg, true);
  }

  public static void enableLoggingToSpecificFiles()
  {
    logToSpecificFiles = true;
    String msg = "MaloWLogger: Logging to specific files for different log levels re-enabled.";
    logForSpecificLevel(LogLevel.INFO, msg, true);
    logForSpecificLevel(LogLevel.WARNING, msg, true);
    logForSpecificLevel(LogLevel.ERROR, msg, true);
  }

  public static void info(String msg)
  {
    log(LogLevel.INFO, msg);
  }

  public static void warning(String msg)
  {
    log(LogLevel.WARNING, msg);
  }

  public static void error(String msg, Exception e)
  {
    log(LogLevel.ERROR, getStringForException(msg, e));
  }

  private static void log(LogLevel level, String msg)
  {
    if (enabled && threshold.level <= level.level)
    {
      logForSpecificLevel(LogLevel.ALL, level.name + ": " + msg, false);
      if (logToSpecificFiles)
      {
        logForSpecificLevel(level, msg, false);
      }
    }
  }

  private static void logForAllLevels(String msg, boolean force)
  {
    logForSpecificLevel(LogLevel.ALL, msg, true);
    logForSpecificLevel(LogLevel.INFO, msg, true);
    logForSpecificLevel(LogLevel.WARNING, msg, true);
    logForSpecificLevel(LogLevel.ERROR, msg, true);
  }

  private static void logForSpecificLevel(LogLevel level, String msg, boolean force)
  {
    msg = getCurrentDateTime() + " - " + msg;
    if ((logToSyso || force) && level.equals(LogLevel.ALL))
    {
      System.out.println(msg);
    }
    if (logToFile || force)
    {
      msg += "\n";
      writeToFile(level.fileName, msg);
    }
  }

  private static void writeToFile(String fileName, String msg)
  {
    try
    {
      new File(FOLDER + fileName).getParentFile().mkdirs();
      Files.write(Paths.get(FOLDER + fileName), msg.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
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

  private static String getStringForException(String msg, Exception e)
  {
    StringBuffer buf = new StringBuffer(
        msg + "\n  Exception in thread \"" + Thread.currentThread().getName() + "\" " + e.getClass().getName() + ": " + e.getMessage());
    List<StackTraceElement> stack = Arrays.asList(e.getStackTrace());
    for (StackTraceElement element : stack)
    {
      buf.append("\n    at " + element.toString());
    }
    buf.append(getStringForCausesRecursively(e.getCause(), stack));
    return buf.toString();
  }

  private static String getStringForCausesRecursively(Throwable cause, List<StackTraceElement> previousStack)
  {
    if (cause == null)
    {
      return "";
    }
    StringBuffer buf = new StringBuffer("\n  Caused by " + cause.getClass().getName() + ": " + cause.getMessage());
    List<StackTraceElement> causeStack = Arrays.asList(cause.getStackTrace());
    for (StackTraceElement causeElement : causeStack)
    {
      buf.append("\n    at " + causeElement.toString());
      if (previousStack.contains(causeElement))
      {
        break;
      }
    }
    buf.append(getStringForCausesRecursively(cause.getCause(), causeStack));
    return buf.toString();
  }
}