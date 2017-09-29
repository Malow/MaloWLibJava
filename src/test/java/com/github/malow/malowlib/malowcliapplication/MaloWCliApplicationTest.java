package com.github.malow.malowlib.malowcliapplication;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;

public class MaloWCliApplicationTest
{

  private static class MaloWCliApplicationForTest extends MaloWCliApplication
  {
    public boolean isApa = false;
    public boolean exitedSpecific = false;

    public MaloWCliApplicationForTest(InputStream in)
    {
      super(in);
    }

    @Command(description = "Sets isApa to true")
    public void apa()
    {
      this.isApa = true;
    }

    @Command(description = "Closes the application and sets exitedSpecific to true")
    @Override
    public void exit()
    {
      this.exitedSpecific = true;
      super.exit();
    }
  }

  @Test
  public void test() throws Exception
  {
    try (PipedInputStream in = new PipedInputStream(); PipedOutputStream out = new PipedOutputStream(in);)
    {
      MaloWCliApplicationForTest cliApp = new MaloWCliApplicationForTest(in);

      Thread inputThread = new Thread()
      {
        @Override
        public void run()
        {
          try
          {
            Thread.sleep(10);
            out.write("apa\n".getBytes());
            out.flush();
            Thread.sleep(10);
            out.write("exit\n".getBytes());
            out.flush();
            Thread.sleep(10);
          }
          catch (Exception e)
          {
            throw new RuntimeException(e);
          }
        }
      };
      inputThread.start();
      cliApp.run();

      assertThat(cliApp.isApa).isTrue();
      assertThat(cliApp.exitedSpecific).isTrue();
    }
  }

  @Test
  public void test2()
  {
    MaloWCliApplicationForTest cliApp = new MaloWCliApplicationForTest(System.in);
    cliApp.run();
  }
}
