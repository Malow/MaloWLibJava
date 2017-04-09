package com.github.malow.malowlib.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.sqlite.SQLiteConfig;

import com.github.malow.malowlib.MaloWLogger;

public class DatabaseConnection
{
  public enum DatabaseType
  {
    SQLITE_MEMORY,
    SQLITE_FILE,
    MYSQL;
  }

  private static class DatabaseConnectionKey
  {

    private DatabaseType databaseType;
    private String databaseName;

    protected DatabaseConnectionKey(DatabaseType databaseType, String databaseName)
    {
      this.databaseType = databaseType;
      this.databaseName = databaseName;
    }

    @Override
    public int hashCode()
    {
      final int prime = 31;
      int result = 1;
      result = prime * result + (this.databaseName == null ? 0 : this.databaseName.hashCode());
      result = prime * result + (this.databaseType == null ? 0 : this.databaseType.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (this == obj)
      {
        return true;
      }
      if (obj == null)
      {
        return false;
      }
      if (this.getClass() != obj.getClass())
      {
        return false;
      }
      DatabaseConnectionKey other = (DatabaseConnectionKey) obj;
      if (this.databaseName == null)
      {
        if (other.databaseName != null)
        {
          return false;
        }
      }
      else if (!this.databaseName.equals(other.databaseName))
      {
        return false;
      }
      if (this.databaseType != other.databaseType)
      {
        return false;
      }
      return true;
    }
  }

  private static Map<DatabaseConnectionKey, Connection> databaseConnections = new HashMap<>();

  public static DatabaseConnection get(DatabaseType databaseType, String databaseName)
  {
    DatabaseConnectionKey key = new DatabaseConnectionKey(databaseType, databaseName);
    Connection connection = databaseConnections.get(key);
    if (connection == null)
    {
      SQLiteConfig config = new SQLiteConfig();
      config.enforceForeignKeys(true);
      try
      {
        switch (databaseType)
        {
          case SQLITE_MEMORY:
            connection = DriverManager.getConnection("jdbc:sqlite::memory:", config.toProperties());
            break;
          case SQLITE_FILE:
            connection = DriverManager.getConnection("jdbc:sqlite:" + databaseName + ".db", config.toProperties());
            break;
          case MYSQL:
            connection = DriverManager.getConnection(
                "jdbc:mysql://192.168.1.110:3306/" + databaseName + "?user=" + "TestUsr" + "&password=" + "test" + "&autoReconnect=true");
            break;
        }
        databaseConnections.put(key, connection);
      }
      catch (SQLException e)
      {
        MaloWLogger.error("Failed to create " + databaseType.toString() + " connection.", e);
      }
    }
    return new DatabaseConnection(databaseType, databaseName, connection);
  }

  private DatabaseType databaseType;
  private String databaseName;
  protected Connection connection;

  private DatabaseConnection(DatabaseType databaseType, String databaseName, Connection connection)
  {
    this.databaseType = databaseType;
    this.databaseName = databaseName;
    this.connection = connection;
  }

  public DatabaseType getDatabaseType()
  {
    return this.databaseType;
  }

  public String getDatabaseName()
  {
    return this.databaseName;
  }

  // Only for tests
  protected static void resetAll()
  {
    databaseConnections.clear();
  }
}
