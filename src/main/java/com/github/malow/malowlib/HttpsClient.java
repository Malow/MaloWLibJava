package com.github.malow.malowlib;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import com.mashape.unirest.http.Unirest;

public class HttpsClient
{
  private static final HttpsClient INSTANCE = new HttpsClient();
  private static CloseableHttpClient httpclient;
  private static String host;

  private HttpsClient()
  {
    if (INSTANCE != null) { throw new IllegalStateException("Already instantiated"); }
    this.init();
  }

  public void init()
  {
    try
    {
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
      @SuppressWarnings("deprecation")
      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build(), SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
      HttpsClient.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
      Unirest.setHttpClient(HttpsClient.httpclient);
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to start HTTPSClient: ", e);
    }
  }

  public void close()
  {
    try
    {
      HttpsClient.httpclient.close();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to close HTTPSClient: ", e);
    }
  }

  public static String sendMessage(String path, String message) throws Exception
  {
    return Unirest.post(HttpsClient.host + path).body(message).asJson().getBody().toString();
  }

  public static void setHost(String host)
  {
    HttpsClient.host = host;
  }
}
