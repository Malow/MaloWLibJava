package com.github.malow.malowlib.network.https;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Login;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.shredzone.acme4j.util.KeyPairUtils;

import com.github.malow.malowlib.MaloWLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Fix for Spring, run in MINGW64: winpty openssl pkcs12 -export -in domain-chain.crt -inkey domain.key -out domain.p12 -name tomcat
 */
public class LetsEncryptDownloader
{
  private static final String FOLDER = "LetsEncryptCerts/";
  private static final File USER_KEY_FILE = new File(FOLDER + "user.key");
  private static final File DOMAIN_KEY_FILE = new File(FOLDER + "domain.key");
  private static final File DOMAIN_CSR_FILE = new File(FOLDER + "domain.csr");
  private static final File DOMAIN_CHAIN_FILE = new File(FOLDER + "domain-chain.crt");

  private static final int KEY_SIZE = 2048;

  private static final boolean STAGING = false;

  private String domain;
  private int port;
  private HttpServer httpServer;

  public static void main(String[] args)
  {
    MaloWLogger.setLoggingThresholdToInfo();
    Security.addProvider(new BouncyCastleProvider());
    USER_KEY_FILE.getParentFile().mkdirs();
    String domain = "malow.duckdns.org";
    int port = 7777;

    LetsEncryptDownloader ct = new LetsEncryptDownloader(domain, port);
    try
    {
      ct.fetchCertificate();
    }
    catch (Exception e)
    {
      MaloWLogger.error("Failed to download certificates", e);
    }
    ct.closeHttpServer();
  }

  /**
   *
   * @param domain
   *          Example: "malow.mooo.com"
   * @param port
   *          The port which port 80 is forwarded to.
   */
  public LetsEncryptDownloader(String domain, int port)
  {
    this.domain = domain;
    this.port = port;
  }

  private static class ChallangeHandler implements HttpHandler
  {
    private String fileContent;

    public ChallangeHandler(String fileContent)
    {
      this.fileContent = fileContent;
    }

    @Override
    public void handle(HttpExchange t) throws IOException
    {
      t.sendResponseHeaders(200, this.fileContent.length());
      OutputStream os = t.getResponseBody();
      os.write(this.fileContent.getBytes(StandardCharsets.UTF_8));
      os.close();
    }
  }

  private void startHttpServerForChallange(String filename, String fileContent) throws Exception
  {
    if (this.httpServer != null)
    {
      this.closeHttpServer();
    }

    this.httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
    this.httpServer.createContext("/.well-known/acme-challenge/" + filename, new ChallangeHandler(fileContent));
    this.httpServer.setExecutor(null); // creates a default executor
    this.httpServer.start();
  }

  private void closeHttpServer()
  {
    if (this.httpServer != null)
    {
      this.httpServer.stop(0);
      this.httpServer = null;
    }
  }

  /*
   * BELOW STOLEN FROM ACME4J ClientTest.java
   */

  /**
   * Generates a certificate for the given domains. Also takes care for the registration process.
   *
   * @param domains
   *          Domains to get a common certificate for
   * @throws Exception
   */
  public void fetchCertificate() throws Exception
  {
    // Load the user key file. If there is no key file, create a new one.
    KeyPair userKeyPair = this.loadOrCreateUserKeyPair();

    // Create a session for Let's Encrypt.
    Session session = new Session(STAGING ? "acme://letsencrypt.org/staging" : "acme://letsencrypt.org");

    // Get the Account.
    // If there is no account yet, create a new one.
    Account acct = this.findOrRegisterAccount(session, userKeyPair);

    // Load or create a key pair for the domains. This should not be the userKeyPair!
    KeyPair domainKeyPair = this.loadOrCreateDomainKeyPair();

    // Order the certificate
    Order order = acct.newOrder().domains(this.domain).create();

    // Perform all required authorizations
    for (Authorization auth : order.getAuthorizations())
    {
      this.authorize(auth);
    }

    // Generate a CSR for all of the domains, and sign it with the domain key pair.
    CSRBuilder csrb = new CSRBuilder();
    csrb.addDomains(this.domain);
    csrb.sign(domainKeyPair);

    // Write the CSR to a file, for later use.
    try (Writer out = new FileWriter(DOMAIN_CSR_FILE))
    {
      csrb.write(out);
    }

    // Order the certificate
    order.execute(csrb.getEncoded());

    // Wait for the order to complete
    try
    {
      int attempts = 10;
      while (order.getStatus() != Status.VALID && attempts-- > 0)
      {
        // Did the order fail?
        if (order.getStatus() == Status.INVALID)
        {
          throw new AcmeException("Order failed... Giving up.");
        }

        // Wait for a few seconds
        Thread.sleep(3000L);

        // Then update the status
        order.update();
      }
    }
    catch (InterruptedException ex)
    {
      MaloWLogger.error("interrupted", ex);
      Thread.currentThread().interrupt();
    }

    // Get the certificate
    Certificate certificate = order.getCertificate();

    MaloWLogger.info("Success! The certificate for " + this.domain + " has been generated!");

    // Write a combined file containing the certificate and chain.
    try (FileWriter fw = new FileWriter(DOMAIN_CHAIN_FILE))
    {
      certificate.writeCertificate(fw);
    }

    // That's all! Configure your web server to use the DOMAIN_KEY_FILE and
    // DOMAIN_CHAIN_FILE for the requested domans.
  }

  /**
   * Loads a user key pair from {@value #USER_KEY_FILE}. If the file does not exist, a new key pair is generated and saved.
   * <p>
   * Keep this key pair in a safe place! In a production environment, you will not be able to access your account again if you should lose the key pair.
   *
   * @return User's {@link KeyPair}.
   */
  private KeyPair loadOrCreateUserKeyPair() throws IOException
  {
    if (USER_KEY_FILE.exists())
    {
      // If there is a key file, read it
      try (FileReader fr = new FileReader(USER_KEY_FILE))
      {
        return KeyPairUtils.readKeyPair(fr);
      }

    }
    // If there is none, create a new key pair and save it
    KeyPair userKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
    try (FileWriter fw = new FileWriter(USER_KEY_FILE))
    {
      KeyPairUtils.writeKeyPair(userKeyPair, fw);
    }
    return userKeyPair;
  }

  /**
   * Loads a domain key pair from {@value #DOMAIN_KEY_FILE}. If the file does not exist, a new key pair is generated and saved.
   *
   * @return Domain {@link KeyPair}.
   */
  private KeyPair loadOrCreateDomainKeyPair() throws IOException
  {
    if (DOMAIN_KEY_FILE.exists())
    {
      try (FileReader fr = new FileReader(DOMAIN_KEY_FILE))
      {
        return KeyPairUtils.readKeyPair(fr);
      }
    }
    KeyPair domainKeyPair = KeyPairUtils.createKeyPair(KEY_SIZE);
    try (FileWriter fw = new FileWriter(DOMAIN_KEY_FILE))
    {
      KeyPairUtils.writeKeyPair(domainKeyPair, fw);
    }
    return domainKeyPair;
  }

  /**
   * Finds your {@link Account} at the ACME server. It will be found by your user's public key. If your key is not known to the server yet, a new account will
   * be created.
   * <p>
   * This is a simple way of finding your {@link Account}. A better way is to get the URL and KeyIdentifier of your new account with
   * {@link Account#getLocation()} {@link Session#getKeyIdentifier()} and store it somewhere. If you need to get access to your account later, reconnect to it
   * via {@link Account#bind(Session, URI)} by using the stored location.
   *
   * @param session
   *          {@link Session} to bind with
   * @return {@link Login} that is connected to your account
   */
  private Account findOrRegisterAccount(Session session, KeyPair accountKey) throws AcmeException
  {
    Account account = new AccountBuilder()
        .agreeToTermsOfService()
        .useKeyPair(accountKey)
        .create(session);
    return account;
  }

  /**
   * Authorize a domain. It will be associated with your account, so you will be able to retrieve a signed certificate for the domain later.
   *
   * @param auth
   *          {@link Authorization} to perform
   * @throws Exception
   */
  private void authorize(Authorization auth) throws Exception
  {
    // The authorization is already valid. No need to process a challenge.
    if (auth.getStatus() == Status.VALID)
    {
      return;
    }

    // Find the desired challenge and prepare it.
    Challenge challenge = this.httpChallenge(auth);

    if (challenge == null)
    {
      throw new AcmeException("No challenge found");
    }

    // If the challenge is already verified, there's no need to execute it again.
    if (challenge.getStatus() == Status.VALID)
    {
      return;
    }

    // Now trigger the challenge.
    challenge.trigger();

    // Poll for the challenge to complete.
    try
    {
      int attempts = 10;
      while (challenge.getStatus() != Status.VALID && attempts-- > 0)
      {
        // Did the authorization fail?
        if (challenge.getStatus() == Status.INVALID)
        {
          throw new AcmeException("Challenge failed... Giving up.");
        }

        // Wait for a few seconds
        Thread.sleep(3000L);

        // Then update the status
        challenge.update();
      }
    }
    catch (InterruptedException ex)
    {
      MaloWLogger.error("interrupted", ex);
      Thread.currentThread().interrupt();
    }

    // All reattempts are used up and there is still no valid authorization?
    if (challenge.getStatus() != Status.VALID)
    {
      throw new AcmeException("Failed to pass the challenge for domain "
          + auth.getIdentifier().getDomain() + ", ... Giving up.");
    }

    MaloWLogger.info("Challenge has been completed. Remember to remove the validation resource.");
  }

  /**
   * Prepares a HTTP challenge.
   * <p>
   * The verification of this challenge expects a file with a certain content to be reachable at a given path under the domain to be tested.
   * <p>
   * This example outputs instructions that need to be executed manually. In a production environment, you would rather generate this file automatically, or
   * maybe use a servlet that returns {@link Http01Challenge#getAuthorization()}.
   *
   * @param auth
   *          {@link Authorization} to find the challenge in
   * @return {@link Challenge} to verify
   * @throws Exception
   */
  public Challenge httpChallenge(Authorization auth) throws Exception
  {
    // Find a single http-01 challenge
    Http01Challenge challenge = auth.findChallenge(Http01Challenge.TYPE);
    if (challenge == null)
    {
      throw new AcmeException("Found no " + Http01Challenge.TYPE + " challenge, don't know what to do...");
    }

    this.startHttpServerForChallange(challenge.getToken(), challenge.getAuthorization());

    return challenge;
  }
}
