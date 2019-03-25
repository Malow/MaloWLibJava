package com.github.malow.malowlib.network.https;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.github.malow.malowlib.GsonSingleton;
import com.github.malow.malowlib.MaloWLogger;

public class SimpleJsonHttpClient
{
  private HttpClient httpClient;
  private String host = "UNDEFINED";
  private boolean acceptAllSsl;

  public SimpleJsonHttpClient(String host, boolean acceptAllSsl)
  {
    this.host = host;
    this.acceptAllSsl = acceptAllSsl;
    this.init();
  }

  public void init()
  {
    try
    {
      if (this.acceptAllSsl)
      {
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new SecureRandom());
        this.httpClient = HttpClient.newBuilder()
            .sslContext(sc)
            .build();
        final Properties props = System.getProperties();
        props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
      }
      else
      {
        this.httpClient = HttpClient.newHttpClient();
      }
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to start HTTPSClient: ", e);
    }
  }

  public String sendGet(String path) throws Exception
  {
    HttpResponse<String> httpResponse = this.httpClient.send(
        HttpRequest
            .newBuilder(new URI(this.host + path))
            .timeout(Duration.ofMillis(10000))
            .GET()
            .build(),
        BodyHandlers.ofString());
    return httpResponse.body();
  }

  public String sendPost(String path, String message) throws Exception
  {
    HttpResponse<String> httpResponse = this.httpClient.send(
        HttpRequest
            .newBuilder(new URI(this.host + path))
            .timeout(Duration.ofMillis(10000))
            .POST(HttpRequest.BodyPublishers.ofString(message))
            .build(),
        BodyHandlers.ofString());
    return httpResponse.body();
  }

  public <RequestClass extends JsonHttpRequest, ResponseClass extends JsonHttpResponse> ResponseClass sendPost(String path, RequestClass request,
      Class<ResponseClass> responseClass)
      throws Exception
  {
    String requestString = GsonSingleton.toJson(request);
    String responseString = this.sendPost(path, requestString);
    return GsonSingleton.fromJson(responseString, responseClass);
  }

  public <RequestClass extends JsonHttpRequest> String sendPost(String path, RequestClass request)
      throws Exception
  {
    String requestString = GsonSingleton.toJson(request);
    return this.sendPost(path, requestString);
  }

  private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager()
  {
    @Override
    public X509Certificate[] getAcceptedIssuers()
    {
      return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate[] certs, String authType)
    {
    }

    @Override
    public void checkServerTrusted(X509Certificate[] certs, String authType)
    {
    }
  } };
}
