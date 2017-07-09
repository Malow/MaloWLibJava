package com.github.malow.malowlib.nextepisodetabopener;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.confighandler.ConfigHandler;
import com.mashape.unirest.http.Unirest;

/**
 * This app will run once every hour, and fetch the HTML from next-episode.com and parse it and then open browsers for each episode that is released "today".
 * Already found episodes is stored in memory and not opened again if found in memory. Make sure to populate the file nextEpConfig.cfg with your data (it's
 * created after you run the program once). To find all the data needed to populate it just open next-episode.com in your browser, login, and then inspect the
 * cookies.
 */
public class NextEpisodeTabOpener
{
  public static void main(String[] args) throws Exception
  {
    MaloWLogger.setLoggingThresholdToInfo();
    NextEpisodeTabOpener parser = new NextEpisodeTabOpener();
    parser.run();
  }


  private static final int SLEEP_DURATION = 1000 * 60 * 60; // 1 hour
  private CloseableHttpClient httpClient = HttpClients.createDefault();
  private List<String> previouslyFoundEpisodes = new ArrayList<String>();
  private String cookieString;
  private String username;

  public NextEpisodeTabOpener() throws Exception
  {
    Unirest.setHttpClient(this.httpClient);
    NextEpisodeTabOpenerConfig cfg = ConfigHandler.loadConfig("nextEpConfig.cfg", NextEpisodeTabOpenerConfig.class);
    this.username = cfg.username;

    Map<String, String> cookies = new HashMap<String, String>();
    cookies.put("PHPSESSID", cfg.PHPSESSID);
    cookies.put("cookie_show_disclaimer", cfg.cookie_show_disclaimer);
    cookies.put("__utmc", cfg.__utmc);
    cookies.put("next_ep_id_secure", cfg.next_ep_id_secure);
    cookies.put("next_ep_user_secure", cfg.next_ep_user_secure);
    cookies.put("next_ep_hash_secure", cfg.next_ep_hash_secure);
    cookies.put("punbb_cookie", cfg.punbb_cookie);
    cookies.put("__utma", cfg.__utma);
    cookies.put("__utmz", cfg.__utmz);
    cookies.put("__utmt", cfg.__utmt);
    cookies.put("__utmb", cfg.__utmb);
    StringBuffer buffer = new StringBuffer();
    for (String key : cookies.keySet())
    {
      buffer.append(key + "=" + cookies.get(key) + ";");
    }
    this.cookieString = buffer.toString();
  }

  public void run()
  {
    while (true)
    {
      try
      {
        this.openNewEpisodesInTabs();
      }
      catch (Exception e)
      {
        MaloWLogger.error("Error:", e);
      }
      try
      {
        Thread.sleep(SLEEP_DURATION);
      }
      catch (InterruptedException e)
      {
      }
    }
  }

  public void openNewEpisodesInTabs() throws Exception
  {
    String response = Unirest.get("http://next-episode.net/").header("Cookie", this.cookieString).asString().getBody().toString();
    if (!response.contains(this.username))
    {
      MaloWLogger.warning("Didn't find " + this.username + " in the html, session probably expired");
      return;
    }
    response = response.substring(response.indexOf("Today's TV Episodes"), response.indexOf("Tomorrow's TV Episodes"));
    Matcher matcher = Pattern.compile(">([a-zA-Z '.,0-9()]+)</a></h3><br>([0-9]+x[0-9]+)</div>").matcher(response);
    boolean found = false;
    while (matcher.find())
    {
      found = true;
      String showName = matcher.group(1);
      String episodeNumber = this.formatEpisodeNumber(matcher.group(2));
      showName = showName.replaceAll(" ", "+");
      String episode = showName + "+" + episodeNumber;
      MaloWLogger.info("Found " + episode);
      if (!this.previouslyFoundEpisodes.contains(episode))
      {
        MaloWLogger.info("Opening tab for " + episode);
        Desktop.getDesktop().browse(new URI("https://rarbg.to/torrents.php?search=" + episode + "&order=size&by=DESC"));
        this.previouslyFoundEpisodes.add(episode);
      }
    }
    if (!found)
    {
      Matcher noEpisodesMatcher = Pattern.compile("No episodes from your watchlist!").matcher(response);
      if (!noEpisodesMatcher.find())
      {
        MaloWLogger.warning("Failed to parse HTML. Check regex matchers and page source. Source searched:\n" + response);
      }
    }
  }

  private String formatEpisodeNumber(String episodeNumber)
  {
    String[] parts = episodeNumber.split("x");
    if (parts[0].length() == 1)
    {
      episodeNumber = "S0" + parts[0];
    }
    else
    {
      episodeNumber = "S" + parts[0];
    }
    if (parts[1].length() == 1)
    {
      episodeNumber += "E0" + parts[1];
    }
    else
    {
      episodeNumber += "E" + parts[1];
    }
    return episodeNumber;
  }
}