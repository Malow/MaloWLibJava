package com.github.malow.malowlib.network.https;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

import com.github.malow.malowlib.MaloWLogger;
import com.mashape.unirest.http.Unirest;

public class HttpsPostClient
{
  private static final HttpsPostClient INSTANCE = new HttpsPostClient();
  private static CloseableHttpClient httpclient;
  private static String host;

  private HttpsPostClient()
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
      HttpsPostClient.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
      Unirest.setHttpClient(HttpsPostClient.httpclient);
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
      HttpsPostClient.httpclient.close();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to close HTTPSClient: ", e);
    }
  }

  public static String sendMessage(String path, String message) throws Exception
  {
    return Unirest.post(HttpsPostClient.host + path).body(message).asJson().getBody().toString();
  }

  public static void setHost(String host)
  {
    HttpsPostClient.host = host;
  }
}
