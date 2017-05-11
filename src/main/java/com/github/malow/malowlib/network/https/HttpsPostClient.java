package com.github.malow.malowlib.network.https;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import com.github.malow.malowlib.MaloWLogger;
import com.mashape.unirest.http.Unirest;

public class HttpsPostClient
{
  private CloseableHttpClient httpClient;
  private String host = "UNDEFINED";
  private boolean acceptAllSsl;

  public HttpsPostClient(String host, boolean acceptAllSsl)
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
        this.httpClient = HttpClients.custom().setSSLContext(sc).setSSLHostnameVerifier(hv).build();
      }
      else
      {
        this.httpClient = HttpClients.createDefault();
      }
      Unirest.setHttpClient(this.httpClient);
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
      this.httpClient.close();
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

  private static HostnameVerifier hv = new HostnameVerifier()
  {
    @Override
    public boolean verify(String hostname, SSLSession session)
    {
      return true;
    }
  };
}
