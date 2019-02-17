package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class DatabaseTransactionConcurrencyTest
{
  protected static class Counter extends DatabaseTableEntity
  {
    public Integer count = 0;
  }

  private static final int NR_OF_RUNNERS = 10;
  private static final int ITERATIONS_PER_RUNNER = 10000;

  private static class Runner extends MaloWProcess
  {
    public int giverId;
    public int takerId;

    public Runner(int giverId, int takerId)
    {
      this.giverId = giverId;
      this.takerId = takerId;
    }

    @Override
    public void life()
    {
      for (int i = 0; i < ITERATIONS_PER_RUNNER; i++)
      {
        try
        {
          Database.executeWithRetries(() ->
          {
            Accessor<Counter> accessor = Database.AccessorsSingleton.getForClass(Counter.class);
            Counter giver = accessor.read(this.giverId);
            Counter taker = accessor.read(this.takerId);
            giver.count--;
            taker.count++;
            accessor.update(giver);
            accessor.update(taker);
          });
        }
        catch (Exception e)
        {
          MaloWLogger.error("Error: ", e);
        }
      }
    }
  }

  @Ignore
  @Test
  public void testConcurrentUpdates() throws Exception
  {
    //DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, "Test").connection.setAutoCommit(false);
    Accessor<Counter> counterAccessor = new Accessor<>(Counter.class, DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, "Test"));
    counterAccessor.createTable();
    Counter giver = counterAccessor.create(new Counter());
    giver.count = NR_OF_RUNNERS * ITERATIONS_PER_RUNNER;
    counterAccessor.update(giver);
    Counter taker = counterAccessor.create(new Counter());

    assertThat(counterAccessor.read(giver.getId()).count).isEqualTo(NR_OF_RUNNERS * ITERATIONS_PER_RUNNER);
    assertThat(counterAccessor.read(taker.getId()).count).isEqualTo(0);

    List<Runner> runners = new ArrayList<>();
    for (int i = 0; i < NR_OF_RUNNERS; i++)
    {
      runners.add(new Runner(giver.getId(), taker.getId()));
    }

    for (Runner runner : runners)
    {
      runner.start();
    }

    for (Runner runner : runners)
    {
      runner.waitUntillDone();
    }

    assertThat(counterAccessor.read(giver.getId()).count).isEqualTo(0);
    assertThat(counterAccessor.read(taker.getId()).count).isEqualTo(NR_OF_RUNNERS * ITERATIONS_PER_RUNNER);
  }
}
