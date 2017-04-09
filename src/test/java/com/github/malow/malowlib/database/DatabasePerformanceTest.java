package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.sqlite.SQLiteConfig;

import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;

public class DatabasePerformanceTest
{
  public static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss][.SSS]");

  public static class Vehicle extends DatabaseTableEntity
  {
    @Unique
    public String licancePlate;
    public Optional<LocalDateTime> purchaseDate = Optional.empty();
    public Optional<Double> value = Optional.empty();

    public Vehicle()
    {
    }

    public Vehicle(String licancePlate)
    {
      this.licancePlate = licancePlate;
    }
  }

  public static class VehicleAccessor extends Accessor<Vehicle>
  {
    public VehicleAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection, Vehicle.class);
    }
  }

  public static class FastVehicleAccessor extends Accessor<Vehicle>
  {
    public FastVehicleAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection, Vehicle.class);
    }

    @Override
    protected void populateStatement(PreparedStatement statement, Vehicle entity) throws Exception
    {
      fastPopulateStatement(statement, entity);
    }

    @Override
    protected void populateEntity(Vehicle entity, ResultSet resultSet) throws Exception
    {
      fastPopulateEntity(entity, resultSet);
    }
  }

  private static final String DATABASE_NAME = "Test";
  private static final int COUNT = 200000;

  @Before
  public void resetDatabase() throws Exception
  {
    new File(DATABASE_NAME + ".db").delete();
    DatabaseConnection.resetAll();
  }

  private static void fastPopulateStatement(PreparedStatement statement, Vehicle entity) throws Exception
  {
    int q = 1;
    statement.setString(q++, entity.licancePlate);
    if (entity.purchaseDate.isPresent())
    {
      statement.setString(q++, entity.purchaseDate.get().toString());
    }
    else
    {
      statement.setString(q++, null);
    }
    if (entity.value.isPresent())
    {
      statement.setDouble(q++, entity.value.get());
    }
    else
    {
      statement.setObject(q++, null);
    }
  }

  private static void fastPopulateEntity(Vehicle entity, ResultSet resultSet) throws Exception
  {
    entity.licancePlate = resultSet.getString("licancePlate");
    String timeStamp = resultSet.getString("purchaseDate");
    if (timeStamp != null)
    {
      entity.purchaseDate = Optional.of(LocalDateTime.parse(timeStamp, DatabasePerformanceTest.formatter));
    }
    else
    {
      entity.purchaseDate = Optional.empty();
    }
    Double value = resultSet.getDouble("value");
    if (resultSet.wasNull())
    {
      entity.value = Optional.empty();
    }
    else
    {
      entity.value = Optional.of(value);
    }
  }

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
        "CREATE TABLE vehicle (id INTEGER PRIMARY KEY AUTOINCREMENT, licancePlate STRING NOT NULL UNIQUE, purchaseDate DATETIME, value DOUBLE)");
    statement.close();
    System.out.println("Create table: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    long before = middle;
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = new Vehicle("a" + i);
      vehicle.purchaseDate = Optional.of(LocalDateTime.now());
      if (i % 2 == 0)
      {
        vehicle.value = Optional.of(i + 0.55);
      }
      PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO vehicle(licancePlate, purchaseDate, value) VALUES (?, ?, ?)",
          Statement.RETURN_GENERATED_KEYS);
      fastPopulateStatement(insertStatement, vehicle);
      insertStatement.executeUpdate();
      insertStatement.getGeneratedKeys().getInt(1);
      insertStatement.close();
    }
    System.out.println("Create entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    for (int i = 0; i < COUNT; i++)
    {
      Statement selectStatement = connection.createStatement();
      ResultSet resultSet = selectStatement.executeQuery("SELECT * FROM vehicle WHERE id = " + (i + 1));
      Vehicle vehicle = new Vehicle();
      fastPopulateEntity(vehicle, resultSet);
      resultSet.close();
      selectStatement.close();
      assertThat(vehicle.licancePlate).isEqualTo("a" + i);
      if (i % 2 == 0)
      {
        assertThat(vehicle.value.get()).isEqualTo(i + 0.55);
      }
      else
      {
        assertThat(vehicle.value.isPresent()).isFalse();
      }
    }
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
      vehicle.purchaseDate = Optional.of(LocalDateTime.now());
      if (i % 2 == 0)
      {
        vehicle.value = Optional.of(i + 0.55);
      }
      vehicleAccessor.create(vehicle);
    }
    System.out.println("Create entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = vehicleAccessor.read(i + 1);
      assertThat(vehicle.licancePlate).isEqualTo("a" + i);
      if (i % 2 == 0)
      {
        assertThat(vehicle.value.get()).isEqualTo(i + 0.55);
      }
      else
      {
        assertThat(vehicle.value.isPresent()).isFalse();
      }
    }
    System.out.println("Read entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    System.out.println("Total read/write: " + (System.nanoTime() - before) / 1000000.0 + "ms");
    System.out.println("");
  }

  @Test
  public void testFrameworkWithOverriddenPopulateMethods() throws Exception
  {
    System.out.println("With fast framework:");
    long middle = System.nanoTime();
    FastVehicleAccessor vehicleAccessor = new FastVehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    System.out.println("Create accessor: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    vehicleAccessor.createTable();
    System.out.println("Create table: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    long before = middle;
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = new Vehicle("a" + i);
      vehicle.purchaseDate = Optional.of(LocalDateTime.now());
      if (i % 2 == 0)
      {
        vehicle.value = Optional.of(i + 0.55);
      }
      vehicleAccessor.create(vehicle);
    }
    System.out.println("Create entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    middle = System.nanoTime();
    for (int i = 0; i < COUNT; i++)
    {
      Vehicle vehicle = vehicleAccessor.read(i + 1);
      assertThat(vehicle.licancePlate).isEqualTo("a" + i);
      if (i % 2 == 0)
      {
        assertThat(vehicle.value.get()).isEqualTo(i + 0.55);
      }
      else
      {
        assertThat(vehicle.value.isPresent()).isFalse();
      }
    }
    System.out.println("Read entries: " + (System.nanoTime() - middle) / 1000000.0 + "ms");
    System.out.println("Total read/write: " + (System.nanoTime() - before) / 1000000.0 + "ms");
    System.out.println("");
  }
}