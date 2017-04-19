package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.Ignore;
import org.junit.Test;

import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;
import com.github.malow.malowlib.database.DatabaseExceptions.ForeignKeyException;
import com.github.malow.malowlib.database.DatabaseExceptions.MissingMandatoryFieldException;
import com.github.malow.malowlib.database.DatabaseExceptions.UniqueException;

public class DatabaseTest extends DatabaseTestFixture
{
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
    p1.fk_car = car.getId();
    p1.fk_bike = bike.getId();
    personAccessor.create(p1);
    Person p2 = new Person("dsa", 3);
    p2.fk_car = car.getId();
    p2.fk_bike = bike.getId() + 100;
    assertThatThrownBy(() ->
    {
      personAccessor.create(p2);
    }).isInstanceOf(ForeignKeyException.class);
    p2.fk_car = car.getId() + 100;
    p2.fk_bike = bike.getId();
    assertThatThrownBy(() ->
    {
      personAccessor.create(p2);
    }).isInstanceOf(ForeignKeyException.class);
    p2.fk_car = car.getId();
    p2.fk_bike = bike.getId();
    personAccessor.create(p2);
    Person p3 = personAccessor.read(p2.getId());
    assertThat(p3.fk_car).isEqualTo(car.getId());
    assertThat(p3.fk_bike).isEqualTo(bike.getId());
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
    }).isInstanceOf(UniqueException.class).hasFieldOrPropertyWithValue("fieldName", "licensePlate").hasFieldOrPropertyWithValue("value", "asd")
        .hasMessage("A row already exists with the field licensePlate containing the unique value asd");
  }

  @Test
  public void testNonPersistedAnnotation() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    Vehicle vehicle = new Vehicle("asd");
    vehicle.cached = 3;
    vehicle = accessor.create(vehicle);
    vehicle = accessor.read(vehicle.getId());
    assertThat(vehicle.cached).isNull();
  }

  @Test
  public void testDelete() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    Vehicle vehicle = accessor.create(new Vehicle("asd"));
    accessor.delete(vehicle.getId());
    assertThat(accessor.getNumberOfEntriesInDatabase()).isEqualTo(0);
    accessor.create(new Vehicle("asd"));
  }

  @Test
  public void testGetNumberOfEntriesInDatabase() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    assertThat(accessor.getNumberOfEntriesInDatabase()).isEqualTo(0);
    accessor.create(new Vehicle("asd"));
    assertThat(accessor.getNumberOfEntriesInDatabase()).isEqualTo(1);
    accessor.create(new Vehicle("dsa"));
    assertThat(accessor.getNumberOfEntriesInDatabase()).isEqualTo(2);
  }

  @Test
  public void testOptionalField() throws Exception
  {
    LocalDateTime date = LocalDateTime.now();
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    accessor.create(new Vehicle("asd"));
    Vehicle v1 = accessor.read(1);
    assertThat(v1.purchaseDate).isNull();
    assertThat(v1.value).isNull();
    Vehicle v2 = new Vehicle("dsad");
    v2.purchaseDate = date;
    v2.value = 2.35;
    accessor.create(v2);
    v2 = accessor.read(2);
    assertThat(v2.purchaseDate).isEqualTo(date);
    assertThat(v2.value).isEqualTo(2.35);
  }

  @Test
  public void testMissingNonOptionalField() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    assertThatThrownBy(() ->
    {
      accessor.create(new Vehicle());
    }).isInstanceOf(MissingMandatoryFieldException.class).hasFieldOrPropertyWithValue("fieldName", "licensePlate")
        .hasMessage("The field licensePlate is mandatory but it was null.");
  }

  @Test
  public void testAccessorsUpdate() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    Vehicle vehicle = accessor.create(new Vehicle("asd"));
    vehicle.licensePlate = "dsa";
    accessor.update(vehicle);
    vehicle = accessor.read(1);
    assertThat(vehicle.getId()).isEqualTo(1);
    assertThat(vehicle.licensePlate).isEqualTo("dsa");
  }

  @Test
  public void testSQLiteInMemory() throws Exception
  {
    VehicleAccessor accessor = new VehicleAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    accessor.createTable();
    accessor.create(new Vehicle("asd"));
    accessor.create(new Vehicle("dsa"));
    Vehicle v1 = accessor.read(1);
    Vehicle v2 = accessor.read(2);
    assertThat(v1.getId()).isEqualTo(1);
    assertThat(v1.licensePlate).isEqualTo("asd");
    assertThat(v2.getId()).isEqualTo(2);
    assertThat(v2.licensePlate).isEqualTo("dsa");
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
    Vehicle v1 = readAccessor.read(1);
    Vehicle v2 = readAccessor.read(2);
    assertThat(v1.getId()).isEqualTo(1);
    assertThat(v1.licensePlate).isEqualTo("asd");
    assertThat(v2.getId()).isEqualTo(2);
    assertThat(v2.licensePlate).isEqualTo("dsa");
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
    Vehicle v1 = readAccessor.read(1);
    Vehicle v2 = readAccessor.read(2);
    assertThat(v1.getId()).isEqualTo(1);
    assertThat(v1.licensePlate).isEqualTo("asd");
    assertThat(v2.getId()).isEqualTo(2);
    assertThat(v2.licensePlate).isEqualTo("dsa");
  }
}
