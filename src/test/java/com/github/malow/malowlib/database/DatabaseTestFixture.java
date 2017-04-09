package com.github.malow.malowlib.database;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;

public class DatabaseTestFixture
{
  protected static class Vehicle extends DatabaseTableEntity
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

  protected static class VehicleAccessor extends Accessor<Vehicle>
  {
    public VehicleAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection, Vehicle.class);
    }
  }

  protected static class Person extends DatabaseTableEntity
  {
    @Unique
    public String name;
    public Integer age;
    @ForeignKey(target = Vehicle.class)
    public Optional<Integer> fk_car;
    @ForeignKey(target = Vehicle.class)
    public Optional<Integer> fk_bike;

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
      super(databaseConnection, Person.class);
    }
  }

  protected static final String DATABASE_NAME = "Test";

  @Before
  public void resetDatabase() throws Exception
  {
    new File(DATABASE_NAME + ".db").delete();
    DatabaseConnection.resetAll();
  }
}
