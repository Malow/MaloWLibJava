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
  private static final String ALL_FILE = "all.log";
  private static LogLevel threshold = LogLevel.INFO;
  private static boolean logToFile = true;
  private static boolean logToSyso = true;
  private static boolean logToSpecificFiles = true;

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
    logSpecificLevel(ALL_FILE, "MaloWLogger: Application Launch", true);
    logSpecificLevel(LogLevel.INFO.fileName, "MaloWLogger: Application Launch", true);
    logSpecificLevel(LogLevel.WARNING.fileName, "MaloWLogger: Application Launch", true);
    logSpecificLevel(LogLevel.ERROR.fileName, "MaloWLogger: Application Launch", true);
  }

  public static void setLoggingThreshold(LogLevel level)
  {
    threshold = level;
    String msg = "MaloWLogger: Logging Threshold set to " + level.name + ". ";
    logSpecificLevel(ALL_FILE, msg, true);
    logSpecificLevel(LogLevel.INFO.fileName, msg, true);
    logSpecificLevel(LogLevel.WARNING.fileName, msg, true);
    logSpecificLevel(LogLevel.ERROR.fileName, msg, true);
  }

  public static void setLogToFile(boolean val)
  {
    logToFile = val;
    String msg = "";
    if (val == false)
    {
      msg = "MaloWLogger: Logging to files disabled.";
    }
    else
    {
      msg = "MaloWLogger: Logging to files re-enabled.";
    }
    logSpecificLevel(ALL_FILE, msg, true);
    logSpecificLevel(LogLevel.INFO.fileName, msg, true);
    logSpecificLevel(LogLevel.WARNING.fileName, msg, true);
    logSpecificLevel(LogLevel.ERROR.fileName, msg, true);
  }

  public static void setLogToSyso(boolean val)
  {
    logToSyso = val;
    if (val == false)
    {
      System.out.println(getCurrentDateTime() + " - " + "MaloWLogger: Logging to system-out disabled.");
    }
    else
    {
      System.out.println(getCurrentDateTime() + " - " + "MaloWLogger: Logging to system-out re-enabled.");
    }
  }

  public static void setLogToSpecificFiles(boolean val)
  {
    logToSpecificFiles = val;
    String msg = "";
    if (val == false)
    {
      msg = "MaloWLogger: Logging to specific files for different log levels disabled, see " + ALL_FILE + " for the combined output of every level.";
    }
    else
    {
      msg = "MaloWLogger: Logging to specific files for different log levels re-enabled.";
    }
    logSpecificLevel(LogLevel.INFO.fileName, msg, true);
    logSpecificLevel(LogLevel.WARNING.fileName, msg, true);
    logSpecificLevel(LogLevel.ERROR.fileName, msg, true);
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
      log(level, getStringForException(msg, e));
    }
  }

  private static void log(LogLevel level, String msg)
  {
    logSpecificLevel(ALL_FILE, level.name + ": " + msg, false);
    if (logToSpecificFiles)
    {
      logSpecificLevel(level.fileName, msg, false);
    }
  }

  private static void logSpecificLevel(String fileName, String msg, boolean force)
  {
    msg = getCurrentDateTime() + " - " + msg;
    if (logToSyso && fileName.equals(ALL_FILE))
    {
      System.out.println(msg);
    }
    if (logToFile || force)
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
    if (cause == null) return "";
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