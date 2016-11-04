package com.github.malow.malowlib.network.https;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.github.malow.malowlib.MaloWLogger;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public abstract class HttpsPostHandler implements HttpHandler
{
  public void handle(HttpExchange t)
  {
    String response = this.handleRequestAndGetResponse(getRequest(t));
    sendResponse(t, 200, response);
  }

  public abstract String handleRequestAndGetResponse(String request);

  private static void sendResponse(HttpExchange t, int code, String response)
  {
    try
    {
      t.sendResponseHeaders(code, response.getBytes().length);
      OutputStream os = t.getResponseBody();
      os.write(response.getBytes());
      os.close();
    }
    catch (IOException e)
    {
      MaloWLogger.error("HttpsPostServer failed when trying to send response: " + response, e);
    }
  }

  protected static HttpsPostRequest createValidJsonRequest(String request, Class<? extends HttpsPostRequest> c)
  {
    HttpsPostRequest req = new Gson().fromJson(request, c);
    if (req != null && req.isValid()) return req;
    else return null;
  }

  private static String getRequest(HttpExchange t)
  {
    String msg = "";
    try
    {
      InputStreamReader isr = new InputStreamReader(t.getRequestBody(), "utf-8");
      BufferedReader br = new BufferedReader(isr);
      int b;
      StringBuilder buf = new StringBuilder();
      while ((b = br.read()) != -1)
      {
        buf.append((char) b);
      }
      br.close();
      isr.close();
      msg = buf.toString();
      return msg;
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed when trying to parse request: " + msg, e);
      return null;
    }
  }
}
