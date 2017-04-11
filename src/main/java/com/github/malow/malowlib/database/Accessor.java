package com.github.malow.malowlib.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.malow.malowlib.database.DatabaseTableEntity.ForeignKey;
import com.github.malow.malowlib.database.DatabaseTableEntity.Optional;
import com.github.malow.malowlib.database.DatabaseTableEntity.Unique;

public abstract class Accessor<Entity extends DatabaseTableEntity>
{
  public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss][.SSS]");
  protected Connection connection;
  protected Class<Entity> entityClass;

  protected String tableName;
  protected String insertString;
  protected String updateString;
  protected List<Field> fields;

  protected PreparedStatement createStatement;
  protected PreparedStatement readStatement;
  protected PreparedStatement updateStatement;
  protected PreparedStatement deleteStatement;

  public Accessor(DatabaseConnection databaseConnection, Class<Entity> entityClass)
  {
    this.connection = databaseConnection.connection;
    this.entityClass = entityClass;
    this.fields = Arrays.asList(this.entityClass.getFields());
    this.tableName = this.entityClass.getSimpleName().toLowerCase();
    this.insertString = "INSERT INTO " + this.tableName + "(";
    this.insertString += this.fields.stream().map(f -> f.getName()).collect(Collectors.joining(", "));
    this.insertString += ") VALUES (";
    this.insertString += this.fields.stream().map(f -> "?").collect(Collectors.joining(", "));
    this.insertString += ")";

    this.updateString = "UPDATE " + this.tableName + " SET ";
    this.updateString += this.fields.stream().map(f -> f.getName() + " = ?").collect(Collectors.joining(", "));
    this.updateString += " WHERE id = ?";
  }

  public Entity create(Entity entity) throws Exception
  {
    try
    {
      if (this.createStatement == null)
      {
        this.createStatement = this.connection.prepareStatement(this.insertString, Statement.RETURN_GENERATED_KEYS);
      }
      this.populateStatement(this.createStatement, entity);
      this.createStatement.executeUpdate();
      entity.setId(this.createStatement.getGeneratedKeys().getInt(1));
      return entity;
    }
    catch (Exception e)
    {
      this.createStatement.close();
      this.createStatement = this.connection.prepareStatement(this.insertString, Statement.RETURN_GENERATED_KEYS);
      throw e;
    }
  }

  public Entity read(Integer id) throws Exception
  {
    try
    {
      if (this.readStatement == null)
      {
        this.readStatement = this.connection.prepareStatement("SELECT * FROM " + this.tableName + " WHERE id = ?");
      }
      this.readStatement.setInt(1, id);
      ResultSet resultSet = this.readStatement.executeQuery();
      if (!resultSet.next())
      {
        return null;
      }
      Entity entity = this.entityClass.newInstance();
      entity.setId(resultSet.getInt("id"));
      this.populateEntity(entity, resultSet);
      resultSet.close();
      return entity;
    }
    catch (Exception e)
    {
      this.readStatement.close();
      this.readStatement = this.connection.prepareStatement("SELECT * FROM " + this.tableName + " WHERE id = ?");
      throw e;
    }
  }

  public void update(Entity entity) throws Exception
  {
    try
    {
      if (this.updateStatement == null)
      {
        this.updateStatement = this.connection.prepareStatement(this.updateString);
      }
      int i = this.populateStatement(this.updateStatement, entity);
      this.updateStatement.setInt(i++, entity.getId());
      int rowCount = this.updateStatement.executeUpdate();
      if (rowCount != 1)
      {
        throw new Exception("Row count wasn't 1 after update: " + rowCount);
      }
    }
    catch (Exception e)
    {
      this.updateStatement.close();
      this.updateStatement = this.connection.prepareStatement(this.updateString);
      throw e;
    }
  }

  public void delete(Integer id) throws Exception
  {
    try
    {
      if (this.deleteStatement == null)
      {
        this.deleteStatement = this.connection.prepareStatement("DELETE FROM " + this.tableName + " WHERE id = ?");
      }
      this.deleteStatement.setInt(1, id);
      int rowCount = this.deleteStatement.executeUpdate();
      if (rowCount != 1)
      {
        throw new Exception("Row count wasn't 1 after delete: " + rowCount);
      }
    }
    catch (Exception e)
    {
      this.deleteStatement.close();
      this.deleteStatement = this.connection.prepareStatement("DELETE FROM " + this.tableName + " WHERE id = ?");
      throw e;
    }
  }

  public int getNumberOfEntriesInDatabase() throws Exception
  {
    Statement statement = this.connection.createStatement();
    ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.tableName);
    int i = 0;
    while (resultSet.next())
    {
      i++;
    }
    resultSet.close();
    statement.close();
    return i;

  }

  protected void dropTable() throws Exception
  {
    Statement statement = this.connection.createStatement();
    statement.executeUpdate("DROP TABLE IF EXISTS " + this.tableName);
    statement.close();
  }

  protected void createTable() throws Exception
  {
    this.dropTable();
    Statement statement = this.connection.createStatement();
    String sql = "CREATE TABLE " + this.tableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT, ";
    List<String> foreignKeys = new ArrayList<>();
    sql += this.fields.stream().map(field ->
    {
      String s = "";
      if (field.isAnnotationPresent(Optional.class))
      {
        s = field.getName() + " " + field.getType().getSimpleName().toUpperCase();
      }
      else
      {
        s = field.getName() + " " + field.getType().getSimpleName().toUpperCase() + " NOT NULL";
      }
      if (field.isAnnotationPresent(Unique.class))
      {
        s += " UNIQUE";
      }
      if (field.isAnnotationPresent(ForeignKey.class))
      {
        String target = field.getAnnotation(ForeignKey.class).target().getSimpleName().toLowerCase();
        foreignKeys.add("FOREIGN KEY(" + field.getName() + ") REFERENCES " + target + "(id)");
      }
      return s;
    }).collect(Collectors.joining(", "));
    if (foreignKeys.size() > 0)
    {
      sql += ", " + foreignKeys.stream().map(foreignKey -> foreignKey).collect(Collectors.joining(", "));
    }
    statement.executeUpdate(sql + ")");
    statement.close();
  }

  protected int populateStatement(PreparedStatement statement, Entity entity) throws Exception
  {
    int i = 1;
    for (Field field : this.fields)
    {
      statement.setObject(i++, field.get(entity));
    }
    return i;
  }

  protected void populateEntity(Entity entity, ResultSet resultSet) throws Exception
  {
    for (Field field : this.fields)
    {
      Object value = this.getValueFromResultSetForField(field, field.getType(), resultSet);
      if (resultSet.wasNull())
      {
        field.set(entity, null);
      }
      else
      {
        field.set(entity, value);
      }
    }
  }

  private Object getValueFromResultSetForField(Field field, Class<?> fieldClass, ResultSet resultSet) throws Exception
  {
    if (fieldClass.equals(String.class))
    {
      return resultSet.getString(field.getName());
    }
    else if (fieldClass.equals(Integer.class))
    {
      return resultSet.getInt(field.getName());
    }
    else if (fieldClass.equals(Double.class))
    {
      return resultSet.getDouble(field.getName());
    }
    else if (fieldClass.equals(LocalDateTime.class))
    {
      String timestamp = resultSet.getString(field.getName());
      if (timestamp != null)
      {
        return LocalDateTime.parse(timestamp, dateFormatter);
      }
      return null;
    }
    else
    {
      throw new Exception("Type not supported: " + fieldClass);
    }
  }
}
