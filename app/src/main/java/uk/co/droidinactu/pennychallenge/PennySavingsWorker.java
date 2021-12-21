package uk.co.droidinactu.pennychallenge;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.android.volley.*;
import com.android.volley.toolbox.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import uk.co.droidinactu.pennychallenge.database.AppDatabase;
import uk.co.droidinactu.pennychallenge.database.SavedOnDate;
import uk.co.droidinactu.pennychallenge.database.SavedOnDateDao;
import uk.co.droidinactu.pennychallenge.starling.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

public class PennySavingsWorker extends Worker {

  public static final String GET_ACCOUNTS_URL = "/api/v2/accounts";
  public static final String GET_ACCOUNT_BALANCE_URL = "/api/v2/accounts/%s/balance";
  public static final String GET_SAVINGS_GOALS_URL = "/api/v2/account/%s/savings-goals";
  public static final String ADD_TO_SAVINGS_GOALS_URL =
      "/api/v2/account/%s/savings-goals/%s/add-money/%s";
  public static final String CREATE_SAVINGS_GOALS_URL = "/api/v2/account/%s/savings-goals";
  public static final String GET_TRANSACTIONS_BETWEEN_URL =
      "/api/v2/feed/account/%s/category/%s/transactions-between";

  public static final String PENNY_CHALLENGE = "PennyChallenge-" + LocalDate.now().getYear();

  private String apiDomainUrl;
  private String accessToken;

  private Context context;
  private RequestQueue requestQueue;

  private Accounts accounts;
  private SavingsGoals savingsGoals;

  public PennySavingsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
    super(context, params);
    this.context = context;
  }

  /**
   * Load key pair
   *
   * @param path
   * @param algorithm
   * @return
   * @throws IOException
   * @throws NoSuchAlgorithmException
   * @throws InvalidKeySpecException
   */
  public static KeyPair LoadKeyPair(Context context, String path, String algorithm)
      throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {

    AssetManager am = context.getAssets();

    // Read Public Key.
    InputStream fisPublicKey = am.open(path + "/public_key.der");
    byte[] encodedPublicKey = new byte[fisPublicKey.available()];
    fisPublicKey.read(encodedPublicKey);
    fisPublicKey.close();

    // Read Private Key.
    InputStream fisPrivateKey = am.open(path + "/private_key.der");
    byte[] encodedPrivateKey = new byte[fisPrivateKey.available()];
    fisPrivateKey.read(encodedPrivateKey);
    fisPrivateKey.close();

    // Generate KeyPair.
    KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

    X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(encodedPublicKey);
    PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

    PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
    PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

    KeyPair objKeyPair = new KeyPair(publicKey, privateKey);

    dumpKeyPair(objKeyPair);

    return objKeyPair;
  }

  /**
   * Save key pair
   *
   * @param path
   * @param keyPair
   * @throws IOException
   */
  public static void SaveKeyPair(String path, KeyPair keyPair) throws IOException {
    // Store Public Key.
    String pathPublicKey = Paths.get(path).resolve("public.der").toString();
    System.out.println(">> SaveKeyPair - pathPublicKey: " + pathPublicKey);

    PublicKey publicKey = keyPair.getPublic();
    X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(publicKey.getEncoded());
    FileOutputStream fosPublic = new FileOutputStream(pathPublicKey);
    fosPublic.write(x509EncodedKeySpec.getEncoded());
    fosPublic.close();

    // Store Private Key.
    String pathPrivateKey = Paths.get(path).resolve("private.der").toString();
    System.out.println(">> SaveKeyPair - pathPrivateKey: " + pathPrivateKey);

    PrivateKey privateKey = keyPair.getPrivate();
    PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(privateKey.getEncoded());
    FileOutputStream fosPrivate = new FileOutputStream(pathPrivateKey);
    fosPrivate.write(pkcs8EncodedKeySpec.getEncoded());
    fosPrivate.close();
  }

  /**
   * Dump key pair
   *
   * @param keyPair
   */
  public static void dumpKeyPair(KeyPair keyPair) {
    PublicKey pub = keyPair.getPublic();
    String publicString = Base64.getEncoder().encodeToString(pub.getEncoded());
    System.out.println(">> dumpKeyPair - Public Key: " + publicString);
    //        System.out.println("Public Key: " + getHexString(pub.getEncoded()));

    PrivateKey priv = keyPair.getPrivate();
    String privateString = Base64.getEncoder().encodeToString(priv.getEncoded());
    System.out.println(">> dumpKeyPair - Private Key: " + privateString);
    //        System.out.println("Private Key: " + getHexString(priv.getEncoded()));
  }

  @Override
  public Result doWork() {
    loadKeys();
    readAccounts(requestQueue);

    // Indicate whether the work finished successfully with the Result
    return Result.success();
  }

  private void loadKeys() {
    try {
      String keyDirPath = "keys";
      Secrets secrets = getSecrets(context);
      this.apiDomainUrl = secrets.getProductionApi();
      String publicKeyUid = secrets.getPublicKeyUid();
      this.accessToken = secrets.getPersonalAccessToken();

      KeyPair keyPair = LoadKeyPair(this.context, keyDirPath, "RSA");

      // Instantiate the cache
      Cache cache = new DiskBasedCache(this.context.getCacheDir(), 1024 * 1024); // 1MB cap

      // Set up the network to use HttpURLConnection as the HTTP client.
      Network network = new BasicNetwork(new HurlStack());

      // Instantiate the RequestQueue with the cache and network.
      requestQueue = Volley.newRequestQueue(this.context);

      // Start the queue
      requestQueue.start();
    } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
      Log.e(MainActivity.TAG, "PennySavingsWorker::loadKeys() " + e.getMessage());
    }
  }

  private Secrets getSecrets(Context context) throws IOException {
    AssetManager am = context.getAssets();

    StringBuilder buf = new StringBuilder();
    InputStream json = context.getAssets().open("keys/secrets.json");
    BufferedReader in = new BufferedReader(new InputStreamReader(json, StandardCharsets.UTF_8));
    String str;
    while ((str = in.readLine()) != null) {
      buf.append(str);
    }
    in.close();

    GsonBuilder gsonb = new GsonBuilder();
    Gson gson = gsonb.create();

    return gson.fromJson(buf.toString(), Secrets.class);
  }

  private void readAccounts(RequestQueue requestQueue) {
    Log.v(MainActivity.TAG, "PennySavingsWorker::readAccounts()");
    JsonObjectRequest jsonArrayRequest =
        new JsonObjectRequest(
            Request.Method.GET,
            this.apiDomainUrl + GET_ACCOUNTS_URL,
            null,
            response -> {
              Log.v(
                  MainActivity.TAG,
                  "PennySavingsWorker::readAccounts()::onResponse() " + response.toString());
              accounts = parseAccounts(response);
              if (accounts.getAccounts().size() > 0) {
                readSavingsGoals(requestQueue, accounts.getAccounts().get(0));
                // FIXME : check if penny savings goal exists
              }
            },
            error -> {
              Log.e(
                  MainActivity.TAG,
                  "PennySavingsWorker::readAccounts()::onErrorResponse() " + error.toString());
              //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
            }) {

          // This is for Headers If You Needed
          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            return getRequestHeaders();
          }
        };
    requestQueue.add(jsonArrayRequest);
  }

  private void readSavingsGoals(RequestQueue requestQueue, Account account) {
    Log.v(MainActivity.TAG, "PennySavingsWorker::readSavingsGoals()");

    String url =
        this.apiDomainUrl
            + new Formatter()
                .format(PennySavingsWorker.GET_SAVINGS_GOALS_URL, account.getAccountUid())
                .toString();

    JsonObjectRequest jsonArrayRequest =
        new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
              Log.v(
                  MainActivity.TAG,
                  "PennySavingsWorker::readSavingsGoals()::onResponse() " + response.toString());
              savingsGoals = parseSavingsGoals(response);
            },
            error -> {
              Log.v(
                  MainActivity.TAG,
                  "PennySavingsWorker::readSavingsGoals()::onErrorResponse() " + error.toString());
              //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
            }) {
          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            return getRequestHeaders();
          }
        };
    requestQueue.add(jsonArrayRequest);
  }

  private void readAccountTransactions(RequestQueue requestQueue, Account account) {
    Log.v(MainActivity.TAG, "PennySavingsWorker::readAccountTransactions()");

    String url =
        this.apiDomainUrl
            + new Formatter()
                .format(
                    PennySavingsWorker.GET_TRANSACTIONS_BETWEEN_URL,
                    account.getAccountUid(),
                    account.getDefaultCategory())
                .toString();

    JsonObjectRequest jsonArrayRequest =
        new JsonObjectRequest(
            Request.Method.GET,
            url,
            null,
            response -> {
              Log.v(
                  MainActivity.TAG,
                  "PennySavingsWorker::readAccountTransactions()::onResponse() "
                      + response.toString());
              savingsGoals = parseSavingsGoals(response);
            },
            error -> {
              Log.v(
                  MainActivity.TAG,
                  "PennySavingsWorker::readAccountTransactions()::onErrorResponse() "
                      + error.toString());
              //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
            }) {
          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            return getRequestHeaders();
          }
        };
    requestQueue.add(jsonArrayRequest);
  }

  private Accounts parseAccounts(JSONObject jsonObj) {
    Accounts accounts = new Accounts();
    try {
      JSONArray jsonObjs = jsonObj.getJSONArray("accounts");
      for (int x = 0; x < jsonObjs.length(); x++) {
        JSONObject acctObj = jsonObjs.getJSONObject(x);
        Account act = new Account();
        act.setAccountUid(acctObj.getString("accountUid"));
        act.setAccountType(acctObj.getString("accountType"));
        act.setDefaultCategory(acctObj.getString("defaultCategory"));
        act.setCurrency(acctObj.getString("currency"));
        String dateTimestr = acctObj.getString("createdAt");
        OffsetDateTime createdDate = OffsetDateTime.parse(dateTimestr);
        act.setCreatedAt(createdDate.toLocalDateTime());
        act.setName(acctObj.getString("name"));

        accounts.addAccount(act);
      }
    } catch (Exception w) {
      Log.e(MainActivity.TAG, "PennySavingsWorker::parseAccounts()::onResponse() " + w.toString());
      //  Toast.makeText(MainActivity.this,w.getMessage(),Toast.LENGTH_LONG).show();
    }
    return accounts;
  }

  private SavingsGoals parseSavingsGoals(JSONObject jsonObj) {
    SavingsGoals savingsGoals = new SavingsGoals();
    try {
      JSONArray jsonObjs = jsonObj.getJSONArray("savingsGoalList");
      for (int x = 0; x < jsonObjs.length(); x++) {
        JSONObject sgObj = jsonObjs.getJSONObject(x);

        SavingsGoal savingsGoal = new SavingsGoal(sgObj.getString("name"));
        savingsGoal.setSavingsGoalUid(sgObj.getString("savingsGoalUid"));
        savingsGoal.setSavedPercentage(sgObj.getInt("savedPercentage"));

        JSONObject sgTrgtObj = sgObj.getJSONObject("target");
        CurrencyAndAmount target = new CurrencyAndAmount();
        target.setCurrency(sgTrgtObj.getString("currency"));
        target.setMinorUnits(sgTrgtObj.getInt("minorUnits"));
        savingsGoal.setTarget(target);

        JSONObject sgTotSvdObj = sgObj.getJSONObject("totalSaved");
        CurrencyAndAmount totalSaved = new CurrencyAndAmount();
        totalSaved.setCurrency(sgTotSvdObj.getString("currency"));
        totalSaved.setMinorUnits(sgTotSvdObj.getInt("minorUnits"));
        savingsGoal.setTotalSaved(totalSaved);

        savingsGoals.addSavingsGoal(savingsGoal);
      }
    } catch (Exception w) {
      Log.v(
          MainActivity.TAG,
          "PennySavingsWorker::parseSavingsGoals()::onResponse() " + w.toString());
      //  Toast.makeText(MainActivity.this,w.getMessage(),Toast.LENGTH_LONG).show();
    }
    return savingsGoals;
  }

  private void roundUpTRansactions() {}

  public void makePennyPayments(
      Account account, AccountBalance accountBalance, SavingsGoal savingsGoal, AppDatabase db) {
    Log.v(MainActivity.TAG, "PennySavingsWorker::makePennyPayments()");

    Calendar cal = Calendar.getInstance();
    int dayNumber = cal.get(Calendar.DAY_OF_YEAR);
    LocalDate startOfYear = LocalDate.of(LocalDate.now().getYear(), 1, 1);

    SavedOnDateDao savedOnDateDao = db.savedOnDateDao();
    List<SavedOnDate> dates = savedOnDateDao.getAll();

    int amountToSave = 0;
    for (int x = 1; x <= dayNumber; x++) {
      LocalDate dateToCheck = startOfYear.plusDays(x - 1);
      List<SavedOnDate> saved = savedOnDateDao.get(dateToCheck);
      if (saved.size() == 0) {
        amountToSave += x;
        SavedOnDate sod = new SavedOnDate();
        sod.dateSaved = dateToCheck;
        sod.amount = x;
        savedOnDateDao.insertAll(sod);
      }
    }
    if (amountToSave > 0) {
      Log.v(
          MainActivity.TAG,
          "PennySavingsWorker::makePennyPayments() saving [" + amountToSave + "] pence");
      if (accountBalance.getEffectiveBalance().getMinorUnits() < amountToSave) {
        Log.e(MainActivity.TAG, "DashboardFragment::makePennyPayments() penny savings up to date");
      } else {
        sendMoneyToSavingsGoal(account, savingsGoal, amountToSave);
        //     sendNotification(amountToSave);
      }
    } else {
      Log.v(MainActivity.TAG, "PennySavingsWorker::makePennyPayments() penny savings up to date");
    }
  }

  private void sendMoneyToSavingsGoal(Account account, SavingsGoal savingsGoal, int amountInPence) {
    Log.v(MainActivity.TAG, "addMoneyToSavingsGoal()");

    UUID transferUid = UUID.randomUUID();
    String url =
        this.apiDomainUrl
            + new Formatter()
                .format(
                    ADD_TO_SAVINGS_GOALS_URL,
                    account.getAccountUid(),
                    savingsGoal.getSavingsGoalUid(),
                    transferUid);

    JSONObject jsonBodyAmt = new JSONObject();
    try {
      JSONObject jsonBody = new JSONObject();
      jsonBody.put("currency", savingsGoal.getTarget().getCurrency());
      jsonBody.put("minorUnits", amountInPence);
      jsonBodyAmt.put("amount", jsonBody);
    } catch (JSONException e) {
      Log.v(MainActivity.TAG, "addMoneyToSavingsGoal() creating body " + e.toString());
    }
    final String requestBody = jsonBodyAmt.toString();

    JsonObjectRequest jsonArrayRequest =
        new JsonObjectRequest(
            Request.Method.PUT,
            url,
            null,
            response -> {
              Log.v(MainActivity.TAG, "onResponse() " + response.toString());
              JSONObject jsonObj = response;
              //   SavingsGoals savingsGoals = parseSavingsGoals(response);
              //  readAccounts();
            },
            error -> {
              Log.v(MainActivity.TAG, "onErrorResponse() " + error.toString());
              //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
            }) {

          @Override
          public Map<String, String> getHeaders() throws AuthFailureError {
            return getRequestHeaders();
          }

          @Override
          public String getBodyContentType() {
            return "application/json; charset=utf-8";
          }

          @Override
          public byte[] getBody() {
            return requestBody == null ? null : requestBody.getBytes(StandardCharsets.UTF_8);
          }
        };
    requestQueue.add(jsonArrayRequest);
  }

  public Map<String, String> getRequestHeaders() throws AuthFailureError {
    Map<String, String> params = new HashMap<>();
    params.put("Accept", "application/json");
    params.put("Content-Type", "application/json; charset=UTF-8");
    params.put("Authorization", String.format("Bearer %s", accessToken));
    return params;
  }
}
