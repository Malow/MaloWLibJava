package com.github.malow.malowlib.newepisodetabopener;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.malowcliapplication.Command;
import com.github.malow.malowlib.malowcliapplication.MaloWCliApplication;
import com.mashape.unirest.http.Unirest;

public class NewEpisodeTabOpener extends MaloWCliApplication
{
  public static void main(String[] args) throws Exception
  {
    MaloWLogger.setLoggingThresholdToInfo();
    NewEpisodeTabOpener program = new NewEpisodeTabOpener();
    program.run();
  }

  private CloseableHttpClient httpClient = HttpClients.createDefault();
  private TabOpenerProcess tabOpener = new TabOpenerProcess();

  public NewEpisodeTabOpener() throws Exception
  {
    Unirest.setHttpClient(this.httpClient);
    this.tabOpener.start();
  }

  @Command(description = "Adds a TvShow to watch for opening of tabs for.")
  public void add(String arguments) throws Exception
  {
    Methods.addSeries(arguments);
  }

  @Command(description = "Closes the application")
  @Override
  public void exit()
  {
    this.tabOpener.closeAndWaitForCompletion();
    super.exit();
  }
}
