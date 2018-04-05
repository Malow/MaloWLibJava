package com.github.malow.malowlib.database;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.database.DatabaseExceptions.ForeignKeyException;
import com.github.malow.malowlib.database.DatabaseExceptions.MissingMandatoryFieldException;
import com.github.malow.malowlib.database.DatabaseExceptions.MultipleRowsReturnedException;
import com.github.malow.malowlib.database.DatabaseExceptions.UnexpectedException;
import com.github.malow.malowlib.database.DatabaseExceptions.UniqueException;
import com.github.malow.malowlib.database.DatabaseExceptions.ZeroRowsReturnedException;
import com.github.malow.malowlib.database.DatabaseTableEntity.ForeignKey;
import com.github.malow.malowlib.database.DatabaseTableEntity.NotPersisted;
import com.github.malow.malowlib.database.DatabaseTableEntity.Optional;
import com.github.malow.malowlib.database.DatabaseTableEntity.Unique;

public abstract class Accessor<Entity extends DatabaseTableEntity>
{
  protected Connection connection;
  protected Class<Entity> entityClass;

  protected String tableName;
  protected String insertString;
  protected String updateString;
  protected List<Field> fields;

  private PreparedStatementPool createStatements;
  private PreparedStatementPool readStatements;
  private PreparedStatementPool updateStatements;
  private PreparedStatementPool deleteStatements;

  @SuppressWarnings("unchecked")
  public Accessor(DatabaseConnection databaseConnection)
  {
    try
    {
      Type genericSuperClass = this.getClass().getGenericSuperclass();
      Type type = ((ParameterizedType) genericSuperClass).getActualTypeArguments()[0];
      this.entityClass = (Class<Entity>) Class.forName(type.getTypeName());
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to get entityClass for accessor", e);
    }

    this.connection = databaseConnection.connection;
    this.fields = Arrays.asList(this.entityClass.getFields());
    this.fields = this.fields.stream().filter(f -> !f.isAnnotationPresent(NotPersisted.class)).collect(Collectors.toList());
    this.tableName = this.entityClass.getSimpleName().toLowerCase();
    this.insertString = "INSERT INTO " + this.tableName + "(";
    this.insertString += this.fields.stream().map(f -> f.getName()).collect(Collectors.joining(", "));
    this.insertString += ") VALUES (";
    this.insertString += this.fields.stream().map(f -> "?").collect(Collectors.joining(", "));
    this.insertString += ")";

    this.updateString = "UPDATE " + this.tableName + " SET ";
    this.updateString += this.fields.stream().map(f -> f.getName() + " = ?").collect(Collectors.joining(", "));
    this.updateString += " WHERE id = ?";

    this.createStatements = this.createPreparedStatementPool(this.insertString, Statement.RETURN_GENERATED_KEYS);
    this.readStatements = this.createPreparedStatementPool("SELECT * FROM " + this.tableName + " WHERE id = ?");
    this.updateStatements = this.createPreparedStatementPool(this.updateString);
    this.deleteStatements = this.createPreparedStatementPool("DELETE FROM " + this.tableName + " WHERE id = ?");
  }

  public Entity create(Entity entity) throws UniqueException, ForeignKeyException, MissingMandatoryFieldException, UnexpectedException
  {
    PreparedStatement statement = null;
    try
    {
      statement = this.createStatements.get();
      this.populateStatement(statement, entity);
      this.createWithPopulatedStatement(statement, entity);
      this.createStatements.add(statement);
      return entity;
    }
    catch (UniqueException | ForeignKeyException | MissingMandatoryFieldException | UnexpectedException e2)
    {
      throw e2;
    }
    catch (Exception e)
    {
      this.logAndReThrowUnexpectedException(
          "Unexpected error when trying to create a " + this.entityClass.getSimpleName() + ": " + entity.toString() + " in accessor", e);
    }
    return null;
  }

  protected Entity createWithPopulatedStatement(PreparedStatement statement, Entity entity) throws Exception
  {
    try
    {
      statement.executeUpdate();
      entity.setId(statement.getGeneratedKeys().getInt(1));
      return entity;
    }
    catch (Exception e)
    {
      if (e.getMessage().contains("A UNIQUE constraint failed (UNIQUE constraint failed:"))
      {
        Matcher matcher = Pattern.compile("A UNIQUE constraint failed \\(UNIQUE constraint failed: ([a-zA-Z '.,0-9]+)\\.([a-zA-Z '.,0-9]+)")
            .matcher(e.getMessage());
        matcher.find();
        String fieldName = matcher.group(2);
        String value = this.fields.stream().filter(f -> f.getName().equals(fieldName)).findFirst().get().get(entity).toString();
        throw new UniqueException(fieldName, value);
      }
      else if (e.getMessage().contains("A foreign key constraint failed (FOREIGN KEY constraint failed)"))
      {
        throw new ForeignKeyException();
      }
      else if (e.getMessage().contains("A NOT NULL constraint failed (NOT NULL constraint failed:"))
      {
        Matcher matcher = Pattern.compile("A NOT NULL constraint failed \\(NOT NULL constraint failed: ([a-zA-Z '.,0-9]+)\\.([a-zA-Z '.,0-9]+)")
            .matcher(e.getMessage());
        matcher.find();
        String fieldName = matcher.group(2);
        throw new MissingMandatoryFieldException(fieldName);
      }
      throw e;
    }
  }

  public Entity read(Integer id) throws ZeroRowsReturnedException, MultipleRowsReturnedException, UnexpectedException
  {
    PreparedStatement statement = null;
    try
    {
      statement = this.readStatements.get();
      statement.setInt(1, id);
      Entity entity = this.readWithPopulatedStatement(statement);
      this.readStatements.add(statement);
      return entity;
    }
    catch (ZeroRowsReturnedException | MultipleRowsReturnedException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      this.closeStatement(statement);
      this.logAndReThrowUnexpectedException(
          "Unexpected error when trying to read a " + this.entityClass.getSimpleName() + " with id " + id + " in accessor", e);
    }
    return null;
  }

  protected Entity readWithPopulatedStatement(PreparedStatement statement) throws Exception
  {
    ResultSet resultSet = statement.executeQuery();
    if (!resultSet.next())
    {
      resultSet.close();
      throw new ZeroRowsReturnedException();
    }
    Entity entity = this.entityClass.newInstance();
    entity.setId(resultSet.getInt("id"));
    this.populateEntity(entity, resultSet);
    if (resultSet.next())
    {
      resultSet.close();
      throw new MultipleRowsReturnedException();
    }
    resultSet.close();
    return entity;
  }

  protected List<Entity> readMultipleWithPopulatedStatement(PreparedStatement statement) throws Exception
  {
    ResultSet resultSet = statement.executeQuery();
    if (!resultSet.next())
    {
      resultSet.close();
      throw new ZeroRowsReturnedException();
    }
    List<Entity> result = new ArrayList<>();
    do
    {
      Entity entity = this.entityClass.newInstance();
      entity.setId(resultSet.getInt("id"));
      this.populateEntity(entity, resultSet);
      result.add(entity);
    } while (resultSet.next());
    resultSet.close();
    return result;
  }

  public void update(Entity entity) throws ZeroRowsReturnedException, MultipleRowsReturnedException, UnexpectedException
  {
    PreparedStatement statement = null;
    try
    {
      statement = this.updateStatements.get();
      int i = this.populateStatement(statement, entity);
      statement.setInt(i++, entity.getId());
      this.updateWithPopulatedStatement(statement);
      this.updateStatements.add(statement);
    }
    catch (ZeroRowsReturnedException | MultipleRowsReturnedException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      this.closeStatement(statement);
      this.logAndReThrowUnexpectedException(
          "Unexpected error when trying to update a " + this.entityClass.getSimpleName() + " with id " + entity.getId() + " in accessor", e);
    }
  }

  public void delete(Entity entity) throws ZeroRowsReturnedException, MultipleRowsReturnedException, UnexpectedException, ForeignKeyException
  {
    this.delete(entity.getId());
  }

  public void delete(Integer id) throws ZeroRowsReturnedException, MultipleRowsReturnedException, UnexpectedException, ForeignKeyException
  {
    PreparedStatement statement = null;
    try
    {
      statement = this.deleteStatements.get();
      statement.setInt(1, id);
      this.updateWithPopulatedStatement(statement);
      this.deleteStatements.add(statement);
    }
    catch (ZeroRowsReturnedException | MultipleRowsReturnedException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      if (e.getMessage().contains("A foreign key constraint failed (FOREIGN KEY constraint failed)"))
      {
        throw new ForeignKeyException();
      }
      this.closeStatement(statement);
      this.logAndReThrowUnexpectedException(
          "Unexpected error when trying to delete a " + this.entityClass.getSimpleName() + " with id " + id + " in accessor", e);
    }
  }

  protected void updateWithPopulatedStatement(PreparedStatement statement) throws Exception
  {
    int rowCount = statement.executeUpdate();
    if (rowCount == 0)
    {
      throw new ZeroRowsReturnedException();
    }
    else if (rowCount > 1)
    {
      throw new MultipleRowsReturnedException();
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

  public void createTable() throws Exception
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
    MaloWLogger.info("Accessor dropped and created table for " + this.entityClass.getSimpleName() + ".");
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
      Object value = ResultSetConverter.getValueFromResultSetForField(field, field.getType(), resultSet);
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

  protected PreparedStatementPool createPreparedStatementPool(String statementString, Integer statementParam)
  {
    return new PreparedStatementPool(this.connection, statementString, statementParam);
  }

  protected PreparedStatementPool createPreparedStatementPool(String statementString)
  {
    return new PreparedStatementPool(this.connection, statementString);
  }

  protected void logAndReThrowUnexpectedException(String msg, Exception e) throws UnexpectedException
  {
    MaloWLogger.error(msg, e);
    throw new UnexpectedException(msg, e);
  }

  protected void closeStatement(PreparedStatement statement)
  {
    try
    {
      if (statement != null)
      {
        statement.close();
      }
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to close statement", e);
    }
  }
}
