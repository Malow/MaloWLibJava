package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import org.junit.Test;
import org.sqlite.SQLiteConfig;

import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;

public class DatabasePerformanceTest extends DatabaseTestFixture
{
  private static int populateStatement(PreparedStatement statement, Vehicle entity) throws Exception
  {
    int q = 1;
    statement.setString(q++, entity.licensePlate);
    if (entity.purchaseDate != null)
    {
      statement.setString(q++, entity.purchaseDate.toString());
    }
    else
    {
      statement.setString(q++, null);
    }
    if (entity.value != null)
    {
      statement.setDouble(q++, entity.value);
    }
    else
    {
      statement.setObject(q++, null);
    }
    return q;
  }

  private static void populateEntity(Vehicle entity, ResultSet resultSet) throws Exception
  {
    entity.licensePlate = resultSet.getString("licensePlate");
    String timeStamp = resultSet.getString("purchaseDate");
    if (timeStamp != null)
    {
      entity.purchaseDate = LocalDateTime.parse(timeStamp, Accessor.dateFormatter);
    }
    else
    {
      entity.purchaseDate = null;
    }
    Double value = resultSet.getDouble("value");
    if (resultSet.wasNull())
    {
      entity.value = null;
    }
    else
    {
      entity.value = value;
    }
  }

  private static final int COUNT = 200000;

  @Test
  public void testWithoutFramework() throws Exception
  {
    System.out.println("Without framework:");
    long middle = System.nanoTime();
    SQLiteConfig config = new SQLiteConfig();
    config.enforceForeignKeys(true);
    Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:", config.toProperties());
    System.out.println("Create connection: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    Statement statement = connection.createStatement();
    statement.executeUpdate("DROP TABLE IF EXISTS vehicle");
    statement.executeUpdate(
        "CREATE TABLE vehicle (id INTEGER PRIMARY KEY AUTOINCREMENT, licensePlate STRING NOT NULL UNIQUE, purchaseDate DATETIME, value DOUBLE)");
    statement.close();
    System.out.println("Create table: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    long before = middle;
    PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO vehicle(licensePlate, purchaseDate, value) VALUES (?, ?, ?)",
        Statement.RETURN_GENERATED_KEYS);
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = new Vehicle("a" + i);
      vehicle.purchaseDate = LocalDateTime.now();
      if (i % 2 == 0)
      {
        vehicle.value = i + 0.55;
      }
      populateStatement(insertStatement, vehicle);
      insertStatement.executeUpdate();
      insertStatement.getGeneratedKeys().getInt(1);
    }
    insertStatement.close();
    System.out.println("Create entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    PreparedStatement selectStatement = connection.prepareStatement("SELECT * FROM vehicle WHERE id = ?");
    for (int i = 0; i < COUNT; i++)
    {
      selectStatement.setInt(1, i + 1);
      ResultSet resultSet = selectStatement.executeQuery();
      Vehicle vehicle = new Vehicle();
      populateEntity(vehicle, resultSet);
      resultSet.close();
      assertThat(vehicle.licensePlate).isEqualTo("a" + i);
      if (i % 2 == 0)
      {
        assertThat(vehicle.value).isEqualTo(i + 0.55);
      }
      else
      {
        assertThat(vehicle.value).isNull();
      }
    }
    selectStatement.close();
    System.out.println("Read entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    System.out.println("Total read/write: " + (System.nanoTime() - before) / 1000000.0 + "ms");
  }

  @Test
  public void testWithFramework() throws Exception
  {
    System.out.println("With framework:");
    long middle = System.nanoTime();
    VehicleAccessor vehicleAccessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    System.out.println("Create accessor: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    vehicleAccessor.createTable();
    System.out.println("Create table: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    long before = middle;
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = new Vehicle("a" + i);
      vehicle.purchaseDate = LocalDateTime.now();
      if (i % 2 == 0)
      {
        vehicle.value = i + 0.55;
      }
      vehicleAccessor.create(vehicle);
    }
    System.out.println("Create entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = vehicleAccessor.read(i + 1);
      assertThat(vehicle.licensePlate).isEqualTo("a" + i);
      if (i % 2 == 0)
      {
        assertThat(vehicle.value).isEqualTo(i + 0.55);
      }
      else
      {
        assertThat(vehicle.value).isNull();
      }
    }
    System.out.println("Read entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    System.out.println("Total read/write: " + (System.nanoTime() - before) / 1000000.0 + "ms");
    System.out.println("");
  }
}
