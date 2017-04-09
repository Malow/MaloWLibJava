package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sqlite.SQLiteException;

import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;

public class DatabaseTest
{
  public static class Vehicle extends DatabaseTableEntity
  {
    @Unique
    public String licancePlate;
    public Optional<LocalDateTime> purchaseDate;
    public Optional<Double> value;

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

  public static class Person extends DatabaseTableEntity
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

  public static class PersonAccessor extends Accessor<Person>
  {
    public PersonAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection, Person.class);
    }
  }

  private static final String DATABASE_NAME = "Test";

  @Before
  public void resetDatabase() throws Exception
  {
    new File(DATABASE_NAME + ".db").delete();
    DatabaseConnection.resetAll();
  }

  @Test
  public void testForeignKeyAnnotation() throws Exception
  {
    VehicleAccessor vehicleAccessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    vehicleAccessor.createTable();
    Vehicle car = vehicleAccessor.create(new Vehicle("asd"));
    Vehicle bike = vehicleAccessor.create(new Vehicle("dsa"));
    PersonAccessor personAccessor = new PersonAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    personAccessor.createTable();
    Person p1 = new Person("asd", 5);
    p1.fk_car = Optional.of(car.getId());
    p1.fk_bike = Optional.of(bike.getId());
    personAccessor.create(p1);
    Person p2 = new Person("dsa", 3);
    p2.fk_car = Optional.of(car.getId());
    p2.fk_bike = Optional.of(bike.getId() + 100);
    assertThatThrownBy(() ->
    {
      personAccessor.create(p2);
    }).isInstanceOf(SQLiteException.class).hasMessageContaining("A foreign key constraint failed (FOREIGN KEY constraint failed)");
    p2.fk_car = Optional.of(car.getId() + 100);
    p2.fk_bike = Optional.of(bike.getId());
    assertThatThrownBy(() ->
    {
      personAccessor.create(p2);
    }).isInstanceOf(SQLiteException.class).hasMessageContaining("A foreign key constraint failed (FOREIGN KEY constraint failed)");
    p2.fk_car = Optional.of(car.getId());
    p2.fk_bike = Optional.of(bike.getId());
    personAccessor.create(p2);
    Person p3 = personAccessor.read(p2.getId());
    assertThat(p3.fk_car.get()).isEqualTo(car.getId());
    assertThat(p3.fk_bike.get()).isEqualTo(bike.getId());
  }

  @Test
  public void testUniqueAnnotation() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    accessor.create(new Vehicle("asd"));
    assertThatThrownBy(() ->
    {
      accessor.create(new Vehicle("asd"));
    }).isInstanceOf(SQLiteException.class).hasMessageContaining(
        "A UNIQUE constraint failed (UNIQUE constraint failed: " + Vehicle.class.getSimpleName().toLowerCase() + ".licancePlate)");
  }

  @Test
  public void testOptionalField() throws Exception
  {
    LocalDateTime date = LocalDateTime.now();
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    accessor.create(new Vehicle("asd"));
    Vehicle c1 = accessor.read(1);
    assertThat(c1.purchaseDate.isPresent()).isFalse();
    assertThat(c1.value.isPresent()).isFalse();
    Vehicle c2 = new Vehicle("dsad");
    c2.purchaseDate = Optional.of(date);
    c2.value = Optional.of(2.35);
    accessor.create(c2);
    c2 = accessor.read(2);
    assertThat(c2.purchaseDate.get()).isEqualTo(date);
    assertThat(c2.value.get()).isEqualTo(2.35);
  }

  @Test
  public void testSQLiteInMemory() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    accessor.create(new Vehicle("asd"));
    accessor.create(new Vehicle("dsa"));
    Vehicle c1 = accessor.read(1);
    Vehicle c2 = accessor.read(2);
    assertThat(c1.getId()).isEqualTo(1);
    assertThat(c1.licancePlate).isEqualTo("asd");
    assertThat(c2.getId()).isEqualTo(2);
    assertThat(c2.licancePlate).isEqualTo("dsa");
  }

  @Test
  public void testSQLiteFilePersistsCorrectly() throws Exception
  {
    VehicleAccessor createAccessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_FILE, DATABASE_NAME));
    createAccessor.createTable();
    createAccessor.create(new Vehicle("asd"));
    createAccessor.create(new Vehicle("dsa"));
    DatabaseConnection.resetAll();
    VehicleAccessor readAccessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_FILE, DATABASE_NAME));
    Vehicle c1 = readAccessor.read(1);
    Vehicle c2 = readAccessor.read(2);
    assertThat(c1.getId()).isEqualTo(1);
    assertThat(c1.licancePlate).isEqualTo("asd");
    assertThat(c2.getId()).isEqualTo(2);
    assertThat(c2.licancePlate).isEqualTo("dsa");
  }

  // Not yet finished, SQL code to be executed on the host for the test to work:
  /*
  DROP DATABASE Test;
  CREATE DATABASE Test;
  USE Test;

  DROP USER TestUsr;
  FLUSH PRIVILEGES;
  CREATE USER TestUsr IDENTIFIED BY 'test';

  GRANT USAGE ON *.* TO 'TestUsr'@'%' IDENTIFIED BY 'test';
  GRANT ALL PRIVILEGES ON Test.* TO 'TestUsr'@'%'WITH GRANT OPTION;
  */
  @Ignore
  @Test
  public void testMysqlPersistsCorrectly() throws Exception
  {
    VehicleAccessor createAccessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.MYSQL, DATABASE_NAME));
    createAccessor.createTable();
    createAccessor.create(new Vehicle("asd"));
    createAccessor.create(new Vehicle("dsa"));
    DatabaseConnection.resetAll();
    VehicleAccessor readAccessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.MYSQL, DATABASE_NAME));
    Vehicle c1 = readAccessor.read(1);
    Vehicle c2 = readAccessor.read(2);
    assertThat(c1.getId()).isEqualTo(1);
    assertThat(c1.licancePlate).isEqualTo("asd");
    assertThat(c2.getId()).isEqualTo(2);
    assertThat(c2.licancePlate).isEqualTo("dsa");
  }
}
