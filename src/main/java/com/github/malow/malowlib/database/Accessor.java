package com.github.malow.malowlib.database;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.malow.malowlib.database.DatabaseTableEntity.ForeignKey;
import com.github.malow.malowlib.database.DatabaseTableEntity.Unique;

public abstract class Accessor<Entity extends DatabaseTableEntity>
{
  private static class EntityField
  {
    Field field;
    boolean isOptional;
    Class<?> clazz;
  }

  public static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss][.SSS]");
  private Connection connection;
  private Class<Entity> entityClass;

  private String tableName;
  private String insertString;
  private String updateString;
  private List<Field> fields;

  private List<EntityField> entityFields;

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

    this.entityFields = new ArrayList<>();
    this.fields.stream().forEach(f ->
    {
      EntityField entityField = new EntityField();
      entityField.field = f;
      entityField.isOptional = f.getType().equals(Optional.class);
      entityField.clazz = this.getClassFromField(f);
      this.entityFields.add(entityField);
    });
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
      if (field.getType().equals(Optional.class))
      {
        s = field.getName() + " " + this.getSqlTypeFromOptionalField(field);
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

  public Entity create(Entity entity) throws Exception
  {
    PreparedStatement statement = this.connection.prepareStatement(this.insertString, Statement.RETURN_GENERATED_KEYS);
    this.populateStatement(statement, entity);
    statement.executeUpdate();
    entity.setId(statement.getGeneratedKeys().getInt(1));
    statement.close();
    return entity;
  }

  protected int populateStatement(PreparedStatement statement, Entity entity) throws Exception
  {
    int i = 1;
    for (EntityField field : this.entityFields)
    {
      if (field.isOptional)
      {
        Optional<?> o = (Optional<?>) field.field.get(entity);
        if (o != null && o.isPresent())
        {
          statement.setObject(i++, o.get());
        }
        else
        {
          statement.setObject(i++, null);
        }
      }
      else
      {
        statement.setObject(i++, field.field.get(entity));
      }
    }
    return i;
  }

  public Entity read(Integer id) throws Exception
  {
    Statement statement = this.connection.createStatement();
    ResultSet resultSet = statement.executeQuery("SELECT * FROM " + this.tableName + " WHERE id = " + id);
    if (!resultSet.next())
    {
      return null;
    }
    Entity entity = this.entityClass.newInstance();
    entity.setId(resultSet.getInt("id"));
    this.populateEntity(entity, resultSet);
    statement.close();
    return entity;
  }

  protected void populateEntity(Entity entity, ResultSet resultSet) throws Exception
  {
    for (EntityField field : this.entityFields)
    {
      Object value = this.getValueFromResultSetForField(field.field, field.clazz, resultSet);
      if (field.isOptional)
      {
        if (resultSet.wasNull())
        {
          field.field.set(entity, Optional.empty());
        }
        else
        {
          field.field.set(entity, Optional.of(value));
        }
      }
      else
      {
        field.field.set(entity, value);
      }
    }
  }

  public void update(Entity entity) throws Exception
  {
    PreparedStatement statement = this.connection.prepareStatement(this.updateString, Statement.RETURN_GENERATED_KEYS);
    int i = this.populateStatement(statement, entity);
    statement.setInt(i++, entity.getId());
    int rowCount = statement.executeUpdate();
    statement.close();
    if (rowCount != 1)
    {
      throw new Exception("Row count wasn't 1 after update: " + rowCount);
    }
  }

  private Class<?> getClassFromField(Field field)
  {
    Class<?> fieldClass = field.getType();
    if (fieldClass.equals(Optional.class))
    {
      fieldClass = this.getClassFromOptionalField(field);
    }
    return fieldClass;
  }

  private String getSqlTypeFromOptionalField(Field field)
  {
    Class<?> clazz = this.getClassFromOptionalField(field);
    if (LocalDateTime.class.equals(clazz))
    {
      return "DATETIME";
    }
    return clazz.getSimpleName().toUpperCase();
  }

  private Class<?> getClassFromOptionalField(Field field)
  {
    try
    {
      Type type = field.getGenericType();
      Field f = type.getClass().getDeclaredField("actualTypeArguments");
      f.setAccessible(true);
      Type type2 = ((Type[]) f.get(type))[0];
      String s = type2.getTypeName();
      return Class.forName(s);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return null;
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
