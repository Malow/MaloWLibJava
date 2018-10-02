package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class DatabaseVersioningConcurrencyTest
{
  protected static class Counter extends DatabaseTableEntity
  {
    public Integer count = 0;
  }

  private static final int NR_OF_RUNNERS = 10;
  private static final int ITERATIONS_PER_RUNNER = 10000;

  private static class Runner extends MaloWProcess
  {
    public int id;

    public Runner(int id)
    {
      this.id = id;
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
            Counter counter = accessor.read(this.id);
            counter.count++;
            accessor.update(counter);
          });
        }
        catch (Exception e)
        {
          MaloWLogger.error("Error: ", e);
        }
      }
    }
  }

  @Test
  public void testConcurrentUpdates() throws Exception
  {
    Accessor<Counter> counterAccessor = new Accessor<>(Counter.class, DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, "Test"));
    counterAccessor.createTable();
    Counter counter = counterAccessor.create(new Counter());
    assertThat(counter.count).isEqualTo(0);

    List<Runner> runners = new ArrayList<>();
    for (int i = 0; i < NR_OF_RUNNERS; i++)
    {
      runners.add(new Runner(counter.getId()));
    }

    for (Runner runner : runners)
    {
      runner.start();
    }

    for (Runner runner : runners)
    {
      runner.waitUntillDone();
    }

    assertThat(counterAccessor.read(counter.getId()).count).isEqualTo(NR_OF_RUNNERS * ITERATIONS_PER_RUNNER);
  }
}
