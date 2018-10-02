package com.github.malow.malowlib.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.malow.malowlib.MaloWLogger;

public class PreparedStatementPool
{
  private Connection connection;
  private String statementString;
  private BlockingDeque<PreparedStatement> statements = new LinkedBlockingDeque<>();
  private Integer statementsParam;

  PreparedStatementPool(Connection connection, String statementString, Integer statementParam)
  {
    this.connection = connection;
    this.statementString = statementString;
    this.statementsParam = statementParam;
  }

  PreparedStatementPool(Connection connection, String statementString)
  {
    this(connection, statementString, null);
  }

  private PreparedStatement get() throws Exception
  {
    PreparedStatement statement = this.statements.pollFirst();
    if (statement != null)
    {
      return statement;
    }
    if (this.statementsParam != null)
    {
      return this.connection.prepareStatement(this.statementString, this.statementsParam);
    }
    else
    {
      return this.connection.prepareStatement(this.statementString);
    }
  }

  private void add(PreparedStatement statement)
  {
    this.statements.add(statement);
  }

  @FunctionalInterface
  public interface StatementFunctionWithReturn<T>
  {
    T apply(PreparedStatement statement) throws Exception;
  }

  @FunctionalInterface
  public interface StatementFunction
  {
    void apply(PreparedStatement statement) throws Exception;
  }

  private void closeStatement(PreparedStatement statement)
  {
    try
    {
      statement.close();
    }
    catch (SQLException e)
    {
      MaloWLogger.error("Failed to close SQL statement in " + this.getClass().getSimpleName(), e);
    }
  }

  public <T> T useStatement(StatementFunctionWithReturn<T> f) throws Exception
  {
    PreparedStatement statement = this.get();
    try
    {
      T obj = f.apply(statement);
      this.add(statement);
      return obj;
    }
    catch (SQLException e)
    {
      MaloWLogger.error(this.getClass().getSimpleName() + " received SQLException, closing statement.", e);
      this.closeStatement(statement);
      throw e;
    }
  }

  public void useStatement(StatementFunction f) throws Exception
  {
    PreparedStatement statement = this.get();
    try
    {
      f.apply(statement);
      this.add(statement);
    }
    catch (SQLException e)
    {
      MaloWLogger.error(this.getClass().getSimpleName() + " received SQLException, closing statement.", e);
      this.closeStatement(statement);
      throw e;
    }
  }
}
