package com.github.malow.malowlib.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

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
    public Integer count = 0;
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
    public Worker(DataAccessor dataAccessor, Integer dataId, int count)
    {
      this.dataAccessor = dataAccessor;
      this.dataId = dataId;
      this.count = count;
    }

    private DataAccessor dataAccessor;
    private Integer dataId;
    private int count;

    @Override
    public void life()
    {
      try
      {
        for (int i = 0; i < this.count; i++)
        {
          NamedMutex mutex = NamedMutexHandler.getAndLockByName("test" + this.dataId);
          Data data = this.dataAccessor.read(this.dataId);
          data.count++;
          this.dataAccessor.update(data);
          mutex.unlock();
        }
      }
      catch (Exception e)
      {
        e.printStackTrace();
        fail(e.getMessage());
      }
    }

    @Override
    public void closeSpecific()
    {
    }
  }

  private static final int THREAD_COUNT = 10;

  @Test
  public void testSameDataUpdates() throws Exception
  {
    final int UPDATES_PER_THREAD = 25000;
    DataAccessor dataAccessor = new DataAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    dataAccessor.createTable();
    Data data = new Data();
    dataAccessor.create(data);

    List<Worker> workers = new ArrayList<Worker>();
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      workers.add(new Worker(dataAccessor, 1, UPDATES_PER_THREAD));
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

  @Test
  public void testDifferentDataUpdates() throws Exception
  {
    final int UPDATES_PER_THREAD = 25000;
    DataAccessor dataAccessor = new DataAccessor(DatabaseConnection.get(DatabaseType.SQLITE_MEMORY, DATABASE_NAME));
    dataAccessor.createTable();

    List<Worker> workers = new ArrayList<Worker>();
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      Data data = new Data();
      dataAccessor.create(data);
      workers.add(new Worker(dataAccessor, data.getId(), UPDATES_PER_THREAD));
    }
    for (Worker worker : workers)
    {
      worker.start();
    }
    for (Worker worker : workers)
    {
      worker.waitUntillDone();
    }
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      Data data = dataAccessor.read(i + 1);
      assertThat(data.count).isEqualTo(UPDATES_PER_THREAD);
    }
  }

  @Test
  public void testDifferentDataUpdatesToFile() throws Exception
  {
    final int UPDATES_PER_THREAD = 25;
    DataAccessor dataAccessor = new DataAccessor(DatabaseConnection.get(DatabaseType.SQLITE_FILE, DATABASE_NAME));
    dataAccessor.createTable();

    List<Worker> workers = new ArrayList<Worker>();
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      Data data = new Data();
      dataAccessor.create(data);
      workers.add(new Worker(dataAccessor, data.getId(), UPDATES_PER_THREAD));
    }
    for (Worker worker : workers)
    {
      worker.start();
    }
    for (Worker worker : workers)
    {
      worker.waitUntillDone();
    }
    for (int i = 0; i < THREAD_COUNT; i++)
    {
      Data data = dataAccessor.read(i + 1);
      assertThat(data.count).isEqualTo(UPDATES_PER_THREAD);
    }
  }
}
