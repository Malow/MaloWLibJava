package com.github.malow.malowlib.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

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

  public PreparedStatement get() throws Exception
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

  public void add(PreparedStatement statement)
  {
    this.statements.add(statement);
  }
}
