package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.malow.malowlib.database.DatabaseConnection.DatabaseType;
import com.github.malow.malowlib.malowprocess.MaloWProcess;
import com.github.malow.malowlib.namedmutex.NamedMutex;
import com.github.malow.malowlib.namedmutex.NamedMutexHandler;

public class DatabaseConcurrencyTest extends DatabaseTestFixture
{
  protected static class Data extends DatabaseTableEntity
  {
    @Unique
    public Integer count;

    public Data()
    {
    }

    public Data(Integer count)
    {
      this.count = count;
    }
  }

  protected static class DataAccessor extends Accessor<Data>
  {
    public DataAccessor(DatabaseConnection databaseConnection)
    {
      super(databaseConnection, Data.class);
    }
  }

  protected static class Worker extends MaloWProcess
  {
    public Worker(DataAccessor dataAccessor)
    {
      this.dataAccessor = dataAccessor;
    }

    private DataAccessor dataAccessor;

    @Override
    public void life()
    {
      try
      {
        for (int i = 0; i < UPDATES_PER_THREAD; i++)
        {
          NamedMutex mutex = NamedMutexHandler.getAndLockByName("test");
          Data data = this.dataAccessor.read(1);
          data.count++;
          this.dataAccessor.update(data);
          mutex.unlock();
        }
      }
      catch (Exception e)
      {
        throw new RuntimeException(e);
      }
    }

    @Override
    public void closeSpecific()
    {
    }
  }

  private static final int THREAD_COUNT = 10;
  private static final int UPDATES_PER_THREAD = 1000;

  @Test
  public void testConcurrency() throws Exception
  {
    DataAccessor dataAccessor = new DataAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    dataAccessor.createTable();
    Data data = new Data();
    data.count = 0;
    dataAccessor.create(data);

    List<Worker> workers = new ArrayList<Worker>();
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      workers.add(new Worker(dataAccessor));
    }
    for (Worker worker : workers)
    {
      worker.start();
    }
    for (Worker worker : workers)
    {
      worker.waitUntillDone();
    }
    data = dataAccessor.read(1);
    assertThat(data.count).isEqualTo(THREAD_COUNT * UPDATES_PER_THREAD);
  }
}
