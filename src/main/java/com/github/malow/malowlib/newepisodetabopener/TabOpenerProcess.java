package com.github.malow.malowlib.newepisodetabopener;

import java.awt.Desktop;
import java.net.URI;
import java.util.List;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.confighandler.ConfigHandler;
import com.github.malow.malowlib.malowprocess.MaloWProcess;

public class TabOpenerProcess extends MaloWProcess
{
  private static final int HOUR_IN_MILLISECONDS = 1000 * 60 * 60; // 1 hour
  private static final String CONFIG_FILE_PATH = "newEpisodeTapOpenerConfig.cfg";

  @Override
  public void life()
  {
    while (this.stayAlive)
    {
      try
      {
        NewEpisodeTabOpenerConfig config = ConfigHandler.loadConfig(CONFIG_FILE_PATH, NewEpisodeTabOpenerConfig.class);
        for (TvShow tvShow : config.watchedTvShows)
        {
          try
          {
            List<Episode> eps = Methods.getNewEpisodes(tvShow.tvdbId);
            for (Episode ep : eps)
            {
              String episodeSearch = tvShow.imdbId + " " + ep.toString();
              MaloWLogger.info("Found new episode: " + tvShow.name + " " + ep.toString());
              episodeSearch = episodeSearch.replaceAll(" ", "+");
              Desktop.getDesktop().browse(new URI("https://rarbg.to/torrents.php?search=" + episodeSearch + "&order=size&by=DESC"));
            }
            Episode latestEp = Methods.getLatestEpisodeFromList(eps);
            if (latestEp != null)
            {
              tvShow.lastFoundEpisode = latestEp;
            }
          }
          catch (Exception e)
          {
            MaloWLogger.error("Error when trying to run for TV show: " + tvShow.name, e);
            Desktop.getDesktop().browse(new URI("https://ErrorInNewEpisodeTabOpener"));
          }
          Thread.sleep(1000);
        }
        ConfigHandler.saveConfig(CONFIG_FILE_PATH, config);
        MaloWLogger.info("Finished looking for new episodes");
        Thread.sleep(4 * HOUR_IN_MILLISECONDS);
      }
      catch (Exception e)
      {
        MaloWLogger.error("Error when trying to run periodic tab-opening:", e);
        try
        {
          Desktop.getDesktop().browse(new URI("https://ErrorInNewEpisodeTabOpener"));
        }
        catch (Exception e1)
        {
        }
        return;
      }
    }
  }

}
