package com.github.malow.malowlib.database;

import java.io.File;
import java.time.LocalDateTime;

import org.junit.Before;

public class DatabaseTestFixture
{
  protected static class Vehicle extends DatabaseTableEntity
  {
    @Unique
    public String licensePlate;
    @Optional
    public LocalDateTime purchaseDate;
    @Optional
    public Double value;
    @NotPersisted
    public Integer cached;

    public Vehicle()
    {
    }

    public Vehicle(String licensePlate)
    {
      this.licensePlate = licensePlate;
    }
  }

  /*
    TODO: Remove these, and use the direct new Accessor() style, but implement new test that tests a specific implementation of an Accessor like this but with also custom statements (like AccServ and GladAr.
   */
  protected static class VehicleAccessor extends Accessor<Vehicle>
  {
    public VehicleAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection);
    }
  }

  protected static class Person extends DatabaseTableEntity
  {
    @Unique
    public String name;
    public Integer age;
    @ForeignKey(target = Vehicle.class)
    @Optional
    public Integer fk_car;
    @ForeignKey(target = Vehicle.class)
    @Optional
    public Integer fk_bike;

    public Person()
    {
    }

    public Person(String name, Integer age)
    {
      this.name = name;
      this.age = age;
    }
  }

  protected static class PersonAccessor extends Accessor<Person>
  {
    public PersonAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection);
    }
  }

  protected static final String DATABASE_NAME = "Test";

  @Before
  public void resetDatabase()
  {
    new File(DATABASE_NAME + ".db").delete();
    DatabaseConnection.resetAll();
  }
}
