package com.github.malow.malowlib.newepisodetabopener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.malow.malowlib.GsonSingleton;
import com.github.malow.malowlib.MaloWLogger;
import com.github.malow.malowlib.confighandler.ConfigHandler;
import com.github.malow.malowlib.newepisodetabopener.apirequests.LoginRequest;
import com.github.malow.malowlib.newepisodetabopener.apiresponses.LoginResponse;
import com.github.malow.malowlib.newepisodetabopener.apiresponses.SearchSeriesResponse;
import com.github.malow.malowlib.newepisodetabopener.apiresponses.SeriesEpisodesResponse;
import com.mashape.unirest.http.Unirest;

public class Methods
{
  private static final String CONFIG_FILE_PATH = "newEpisodeTapOpenerConfig.cfg";
  private static final String API_BASE_URL = "https://api.thetvdb.com";

  public static void addSeries(String imdbId) throws Exception
  {
    TvShow tvShow = new TvShow(imdbId);
    NewEpisodeTabOpenerConfig config = ConfigHandler.loadConfig(CONFIG_FILE_PATH, NewEpisodeTabOpenerConfig.class);
    if (config.watchedTvShows.contains(tvShow))
    {
      throw new Exception("Already watching that TvShow");
    }
    authenticate();

    String responseJson = Unirest.get(API_BASE_URL + "/search/series")
        .header("Authorization", "Bearer " + config.apiToken)
        .queryString("imdbId", imdbId)
        .asString().getBody().toString();
    SearchSeriesResponse response = GsonSingleton.fromJson(responseJson, SearchSeriesResponse.class);
    SearchSeriesResponse.Data data = response.data.stream().filter(d -> !d.seriesName.contains("***Duplicate")).findFirst().get();
    tvShow.name = data.seriesName;
    tvShow.tvdbId = data.id;
    tvShow.lastFoundEpisode = getLatestEpisode(tvShow.tvdbId);
    config.watchedTvShows.add(tvShow);
    ConfigHandler.saveConfig(CONFIG_FILE_PATH, config);
    MaloWLogger.info("Successfully added " + tvShow.name + " to watched TvShows");
  }

  public static List<Episode> getNewEpisodes(String tvdbSeriesId) throws Exception
  {
    authenticate();

    NewEpisodeTabOpenerConfig config = ConfigHandler.loadConfig(CONFIG_FILE_PATH, NewEpisodeTabOpenerConfig.class);
    TvShow tvShow = config.watchedTvShows.stream().filter(show -> show.tvdbId.equals(tvdbSeriesId)).findFirst().get();

    List<Episode> episodes = getEpisodesForTvShow(tvdbSeriesId);
    episodes = episodes.stream().filter(e -> e.isNewerThan(tvShow.lastFoundEpisode)).collect(Collectors.toList());
    return episodes;
  }

  public static Episode getLatestEpisodeFromList(List<Episode> episodes)
  {
    if (episodes.isEmpty())
    {
      return null;
    }
    Episode latestEpisode = episodes.get(0);
    for (Episode episode : episodes)
    {
      if (episode.isNewerThan(latestEpisode))
      {
        latestEpisode = episode;
      }
    }
    return latestEpisode;
  }

  private static Episode getLatestEpisode(String tvdbSeriesId) throws Exception
  {
    return getLatestEpisodeFromList(getEpisodesForTvShow(tvdbSeriesId));
  }

  private static List<Episode> getEpisodesForTvShow(String tvdbSeriesId) throws Exception
  {
    List<Episode> episodes = new ArrayList<>();
    NewEpisodeTabOpenerConfig config = ConfigHandler.loadConfig(CONFIG_FILE_PATH, NewEpisodeTabOpenerConfig.class);
    boolean finished = false;
    int nextPage = 1;
    while (!finished)
    {
      String responseJson = Unirest.get(API_BASE_URL + "/series/" + tvdbSeriesId + "/episodes")
          .header("Authorization", "Bearer " + config.apiToken)
          .queryString("page", nextPage)
          .asString().getBody().toString();
      SeriesEpisodesResponse response = GsonSingleton.fromJson(responseJson, SeriesEpisodesResponse.class);
      for (SeriesEpisodesResponse.Data data : response.data)
      {
        if (data.firstAired.isEmpty() || data.firstAired.equals("0000-00-00"))
        {
          continue;
        }

        LocalDate aired = LocalDate.parse(data.firstAired, DateTimeFormatter.ofPattern("uuuu-MM-dd"));
        if (aired.isBefore(LocalDate.now()))
        {
          episodes.add(new Episode(data.airedSeason, data.airedEpisodeNumber, aired));
        }
      }
      if (response.links.next == null)
      {
        finished = true;
      }
      else
      {
        nextPage = response.links.next;
      }
    }
    return episodes;
  }

  private static void authenticate() throws Exception
  {
    try
    {
      refreshToken();
    }
    catch (Exception e)
    {
      MaloWLogger.info("Token refresh failed: " + e.toString());
      login();
    }
  }

  private static void login() throws Exception
  {
    NewEpisodeTabOpenerConfig config = ConfigHandler.loadConfig(CONFIG_FILE_PATH, NewEpisodeTabOpenerConfig.class);
    String responseJson = Unirest.post(API_BASE_URL + "/login")
        .header("Content-Type", "application/json")
        .body(GsonSingleton.toJson(new LoginRequest(config.apiKey, config.userKey, config.userName)))
        .asJson().getBody().toString();

    LoginResponse response = GsonSingleton.fromJson(responseJson, LoginResponse.class);
    if (response.token == null)
    {
      throw new Exception("Error when logging in, response: " + responseJson);
    }
    MaloWLogger.info("Successfully logged in");
    config.apiToken = response.token;
    config.lastApiTokenRefresh = LocalDateTime.now();
    ConfigHandler.saveConfig(CONFIG_FILE_PATH, config);
  }


  private static void refreshToken() throws Exception
  {
    NewEpisodeTabOpenerConfig config = ConfigHandler.loadConfig(CONFIG_FILE_PATH, NewEpisodeTabOpenerConfig.class);
    if (config.lastApiTokenRefresh.isBefore(LocalDateTime.now().minusHours(12)))
    {
      String responseJson = Unirest.get(API_BASE_URL + "/refresh_token")
          .header("Authorization", "Bearer " + config.apiToken)
          .header("Content-Type", "application/json")
          .asJson().getBody().toString();

      LoginResponse response = GsonSingleton.fromJson(responseJson, LoginResponse.class);
      if (response.token == null)
      {
        throw new Exception("Error when refreshing token, response: " + responseJson);
      }
      MaloWLogger.info("Refreshed token");
      config.apiToken = response.token;
      config.lastApiTokenRefresh = LocalDateTime.now();
      ConfigHandler.saveConfig(CONFIG_FILE_PATH, config);
    }
  }
}
