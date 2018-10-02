package com.github.malow.malowlib.database;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.MaloWUtils;
import com.github.malow.malowlib.database.DatabaseExceptions.ForeignKeyException;
import com.github.malow.malowlib.database.DatabaseExceptions.MissingMandatoryFieldException;
import com.github.malow.malowlib.database.DatabaseExceptions.MultipleRowsReturnedException;
import com.github.malow.malowlib.database.DatabaseExceptions.SimultaneousModificationException;
import com.github.malow.malowlib.database.DatabaseExceptions.UnexpectedException;
import com.github.malow.malowlib.database.DatabaseExceptions.UniqueException;
import com.github.malow.malowlib.database.DatabaseExceptions.ZeroRowsReturnedException;
import com.github.malow.malowlib.database.DatabaseTableEntity.ForeignKey;
import com.github.malow.malowlib.database.DatabaseTableEntity.NotPersisted;
import com.github.malow.malowlib.database.DatabaseTableEntity.Optional;
import com.github.malow.malowlib.database.DatabaseTableEntity.Unique;

public class Accessor<Entity extends DatabaseTableEntity>
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

  public Accessor(Class<Entity> clazz, DatabaseConnection databaseConnection)
  {
    this.connection = databaseConnection.connection;
    this.entityClass = clazz;
    this.init();
  }

  protected Accessor(DatabaseConnection databaseConnection)
  {
    this.connection = databaseConnection.connection;
    try
    {
      this.entityClass = MaloWUtils.getGenericClassFor(this);
    }
    catch (ClassNotFoundException e)
    {
      MaloWLogger.error("Failed to get entityClass for accessor", e);
    }
    this.init();
  }

  private void init()
  {
    try
    {
      Database.AccessorsSingleton.register(this);
    }
    catch (ClassNotFoundException e)
    {
      MaloWLogger.error("Failed to register Accessor to AccessorsSingleton for class " + this.getEntityClass().getSimpleName(), e);
    }
    this.fields = Arrays.asList(this.entityClass.getFields());
    this.fields = this.fields.stream().filter(f -> !f.isAnnotationPresent(NotPersisted.class)).collect(Collectors.toList());
    this.tableName = this.entityClass.getSimpleName().toLowerCase();
    this.insertString = "INSERT INTO " + this.tableName + "(version, "
        + this.fields.stream().map(f -> f.getName()).collect(Collectors.joining(", "))
        + ") VALUES (?, "
        + this.fields.stream().map(f -> "?").collect(Collectors.joining(", "))
        + ")";

    this.updateString = "UPDATE " + this.tableName + " SET version = ?, "
        + this.fields.stream().map(f -> f.getName() + " = ?").collect(Collectors.joining(", "))
        + " WHERE id = ? AND version = ?";

    this.createStatements = this.createPreparedStatementPool(this.insertString, Statement.RETURN_GENERATED_KEYS);
    this.readStatements = this.createPreparedStatementPool("SELECT * FROM " + this.tableName + " WHERE id = ?");
    this.updateStatements = this.createPreparedStatementPool(this.updateString);
    this.deleteStatements = this.createPreparedStatementPool("DELETE FROM " + this.tableName + " WHERE id = ?");
  }

  public Class<Entity> getEntityClass()
  {
    return this.entityClass;
  }

  public Entity create(Entity entity) throws UniqueException, ForeignKeyException, MissingMandatoryFieldException, UnexpectedException
  {
    try
    {
      return this.createStatements.useStatement(statement ->
      {
        statement.setObject(1, entity.getVersion());
        this.populateStatement(statement, entity, 2);
        return this.createWithPopulatedStatement(statement, entity);
      });
    }
    catch (UniqueException | ForeignKeyException | MissingMandatoryFieldException | UnexpectedException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw this.logAndCreateUnexpectedException(
          "Unexpected error when trying to create a " + this.entityClass.getSimpleName() + ": " + entity.toString() + " in accessor", e);
    }
  }

  protected Entity createWithPopulatedStatement(PreparedStatement statement, Entity entity) throws Exception
  {
    try
    {
      this.executeSingleUpdate(statement);
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
    try
    {
      return this.readStatements.useStatement(statement ->
      {
        statement.setInt(1, id);
        return this.readWithPopulatedStatement(statement);
      });
    }
    catch (ZeroRowsReturnedException | MultipleRowsReturnedException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw this.logAndCreateUnexpectedException(
          "Unexpected error when trying to read a " + this.entityClass.getSimpleName() + " with id " + id + " in accessor", e);
    }
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
    entity.setVersion(resultSet.getInt("version"));
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

  public void update(Entity entity) throws SimultaneousModificationException, MultipleRowsReturnedException, UnexpectedException
  {
    try
    {
      this.updateStatements.useStatement(statement ->
      {
        statement.setInt(1, entity.getVersion() + 1);
        int i = this.populateStatement(statement, entity, 2);
        statement.setInt(i++, entity.getId());
        statement.setInt(i++, entity.getVersion());
        this.executeSingleUpdate(statement);
        entity.incrementVersion();
      });
    }
    catch (ZeroRowsReturnedException e)
    {
      throw new SimultaneousModificationException();
    }
    catch (MultipleRowsReturnedException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw this.logAndCreateUnexpectedException(
          "Unexpected error when trying to update a " + this.entityClass.getSimpleName() + " with id " + entity.getId() + " in accessor", e);
    }
  }

  public void delete(Entity entity) throws ZeroRowsReturnedException, MultipleRowsReturnedException, UnexpectedException, ForeignKeyException
  {
    this.delete(entity.getId());
  }

  public void delete(Integer id) throws ZeroRowsReturnedException, MultipleRowsReturnedException, UnexpectedException, ForeignKeyException
  {
    try
    {
      this.deleteStatements.useStatement(statement ->
      {
        statement.setInt(1, id);
        this.deleteWithPopulatedStatement(statement);
      });
    }
    catch (ZeroRowsReturnedException | MultipleRowsReturnedException | ForeignKeyException e)
    {
      throw e;
    }
    catch (Exception e)
    {
      throw this.logAndCreateUnexpectedException(
          "Unexpected error when trying to delete a " + this.entityClass.getSimpleName() + " with id " + id + " in accessor", e);
    }
  }

  protected void deleteWithPopulatedStatement(PreparedStatement statement)
      throws ZeroRowsReturnedException, MultipleRowsReturnedException, ForeignKeyException, SQLException
  {
    try
    {
      this.executeSingleUpdate(statement);
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
      throw e;
    }
  }

  protected void executeSingleUpdate(PreparedStatement statement) throws ZeroRowsReturnedException, MultipleRowsReturnedException, SQLException
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
    MaloWLogger.info("Accessor dropped table for " + this.entityClass.getSimpleName() + ".");
  }

  public void createTable() throws Exception
  {
    this.dropTable();
    Statement statement = this.connection.createStatement();
    String sql = "CREATE TABLE " + this.tableName
        + " (id INTEGER PRIMARY KEY AUTOINCREMENT, "
        + "version INTEGER NOT NULL, ";
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
    MaloWLogger.info("Accessor created table for " + this.entityClass.getSimpleName() + ".");
  }

  protected int populateStatement(PreparedStatement statement, Entity entity, int startIndex) throws Exception
  {
    for (Field field : this.fields)
    {
      statement.setObject(startIndex++, field.get(entity));
    }
    return startIndex;
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

  protected UnexpectedException logAndCreateUnexpectedException(String msg, Exception e)
  {
    MaloWLogger.error(msg, e);
    return new UnexpectedException(msg, e);
  }
}
