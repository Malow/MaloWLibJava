package com.github.malow.malowlib.malowcliapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;

public class MaloWCliApplicationTest
{
  private static class MaloWCliApplicationForTest extends MaloWCliApplication
  {
    public boolean isMonkey = false;
    public boolean exitedSpecific = false;
    public String text = "";

    public MaloWCliApplicationForTest(InputStream in)
    {
      super(in);
    }

    @Command(description = "Sets isMonkey to true")
    public void monkey()
    {
      this.isMonkey = true;
    }

    @Command(description = "Sets text to input")
    public void say(String text)
    {
      this.text = text;
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
  public void testSuccessfully() throws Exception
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
            out.write("monkey\n".getBytes());
            out.flush();
            Thread.sleep(10);
            out.write("say hello\n".getBytes());
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

      assertThat(cliApp.isMonkey).isTrue();
      assertThat(cliApp.text).isEqualTo("hello");
      assertThat(cliApp.exitedSpecific).isTrue();
    }
  }

  private static class BadMaloWCliApplicationForTest extends MaloWCliApplication
  {
    @Command(description = "Should throw exception due to bad parameter")
    public void bad(int text)
    {
    }
  }

  @Test
  public void testThatExceptionIsThrownForBadCommandParameters() throws Exception
  {
    assertThatThrownBy(() ->
    {
      new BadMaloWCliApplicationForTest();
    }).isInstanceOf(RuntimeException.class)
        .hasMessage("Unsupported parameter type for command-method bad, only 1 String is supported.");
  }
}
