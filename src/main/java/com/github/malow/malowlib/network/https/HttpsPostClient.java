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
  private CloseableHttpClient httpclient;
  private String host = "UNDEFINED";

  public HttpsPostClient(String host)
  {
    this.host = host;
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
      this.httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
      Unirest.setHttpClient(this.httpclient);
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
      this.httpclient.close();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to close HTTPSClient: ", e);
    }
  }

  public String sendMessage(String path, String message) throws Exception
  {
    return Unirest.post(this.host + path).body(message).asJson().getBody().toString();
  }
}
