package uk.co.droidinactu.pennychallenge.ui.dashboard;

import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.AuthFailureError;
import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import uk.co.droidinactu.pennychallenge.MainActivity;
import uk.co.droidinactu.pennychallenge.PennySavingsWorker;
import uk.co.droidinactu.pennychallenge.database.SavedOnDate;
import uk.co.droidinactu.pennychallenge.database.SavedOnDateDao;
import uk.co.droidinactu.pennychallenge.starling.Account;
import uk.co.droidinactu.pennychallenge.starling.AccountBalance;
import uk.co.droidinactu.pennychallenge.starling.Accounts;
import uk.co.droidinactu.pennychallenge.starling.CurrencyAndAmount;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoal;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoals;
import uk.co.droidinactu.pennychallenge.starling.Secrets;

public class DashboardViewModel extends AndroidViewModel {

    private String apiDomainUrl;
    private String accessToken;

    private Context context;
    private RequestQueue requestQueue;

    private MutableLiveData<Accounts> starlingAccounts;
    private MutableLiveData<SavingsGoals> starlingSavingsGoals;
    private MutableLiveData<AccountBalance> starlingAccountBalance;

    public DashboardViewModel(Application application)
            throws NoSuchAlgorithmException, IOException, InvalidKeySpecException {
        super(application);
        if (starlingAccountBalance == null) {
            starlingAccountBalance = new MutableLiveData<>();
        }
        if (starlingSavingsGoals == null) {
            starlingSavingsGoals = new MutableLiveData<>();
        }
        if (starlingAccounts == null) {
            starlingAccounts = new MutableLiveData<>();
            String keyDirPath = "keys";
            try {
                Secrets secrets = getSecrets(getApplication());
                this.context = getApplication().getApplicationContext();
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
                loadAccounts();
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                Log.e(MainActivity.TAG, "DashboardViewModel() " + e.getMessage());
            }
        }
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

    private void loadAccounts() {
        Log.v(MainActivity.TAG, "DashboardViewModel::loadAccounts()");
        readAccounts();
    }

    public void loadAccountBalance(Account account) {
        Log.v(MainActivity.TAG, "DashboardViewModel::loadAccountBalance()");
        readAccountBalance(account);
    }

    public void loadSavingsGoals(Account account) {
        Log.v(MainActivity.TAG, "DashboardViewModel::loadSavingsGoals()");
        readSavingsGoals(account);
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

        Secrets secrets = gson.fromJson(buf.toString(), Secrets.class);
        return secrets;
    }

    public LiveData<Accounts> getAccounts() {
        return starlingAccounts;
    }

    public LiveData<AccountBalance> getAccountBalance() {
        return starlingAccountBalance;
    }

    public LiveData<SavingsGoals> getSavingsGoals() {
        return starlingSavingsGoals;
    }

    public void addMoneyToSavingsGoal(Account account, SavingsGoal savingsGoal, AccountBalance accountBalance, SavedOnDateDao savedOnDateDao) {
        Log.v(MainActivity.TAG, "DashboardViewModel::loadSavingsGoals()");

        Calendar cal = Calendar.getInstance();
        int dayNumber = cal.get(Calendar.DAY_OF_YEAR);
        LocalDate startOfYear = LocalDate.of(LocalDate.now().getYear(), 1, 1);

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
            Log.v(MainActivity.TAG, "PennySavingsWorker::makePennyPayments() saving [" + amountToSave + "] pence");
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

    public void addMoneyToSavingsGoal(Account account, SavingsGoal savingsGoal, int amount) {
        Log.v(MainActivity.TAG, "DashboardViewModel::loadSavingsGoals()");
        sendMoneyToSavingsGoal(account, savingsGoal, amount);
    }

    private void readAccounts() {
        Log.v(MainActivity.TAG, "getAccounts()");
        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(
                Request.Method.GET,
                this.apiDomainUrl + PennySavingsWorker.GET_ACCOUNTS_URL,
                null,
                response -> {
                    Log.v(MainActivity.TAG, "onResponse() " + response.toString());
                    JSONObject jsonObj = response;
                    starlingAccounts.setValue(parseAccountsResponse(jsonObj));
                }, error -> {
            Log.e(MainActivity.TAG, "onErrorResponse() " + error.toString());
            //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
        }) {

            //This is for Headers If You Needed
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getRequestHeaders();
            }
        };
        requestQueue.add(jsonArrayRequest);
    }

    private void readSavingsGoals(Account account) {
        Log.v(MainActivity.TAG, "getSavingsGoals()");

        String url = this.apiDomainUrl
                + new Formatter().format(
                PennySavingsWorker.GET_SAVINGS_GOALS_URL,
                account.getAccountUid()
        ).toString();

        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.v(MainActivity.TAG, "readSavingsGoals()::onResponse() " + response.toString());
                    JSONObject jsonObj = response;
                    starlingSavingsGoals.setValue(parseSavingsGoals(jsonObj));
                }, error -> {
            Log.v(MainActivity.TAG, "onErrorResponse() " + error.toString());
            SavingsGoals sgls = new SavingsGoals();
            sgls.addSavingsGoal(new SavingsGoal(PennySavingsWorker.PENNY_CHALLENGE));
            starlingSavingsGoals.setValue(sgls);
            //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getRequestHeaders();
            }
        };
        requestQueue.add(jsonArrayRequest);
    }

    public void readAccountBalance(Account account) {
        Log.v(MainActivity.TAG, "getAccountBalance()");

        UUID transferUid = UUID.randomUUID();
        String url = this.apiDomainUrl + new Formatter().format(
                PennySavingsWorker.GET_ACCOUNT_BALANCE_URL,
                account.getAccountUid()
        );

        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    Log.v(MainActivity.TAG, "onResponse() " + response.toString());
                    JSONObject jsonObj = response;
                    starlingAccountBalance.setValue(parseAccountBalance(jsonObj));
                }, error -> {
            Log.v(MainActivity.TAG, "onErrorResponse() " + error.toString());
            //   Toast.makeText(MainActivity.this,error.getMessage(),Toast.LENGTH_LONG).show();
        }) {

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return getRequestHeaders();
            }
        };
        requestQueue.add(jsonArrayRequest);
    }

    private Accounts parseAccountsResponse(JSONObject jsonObj) {
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
            Log.e(MainActivity.TAG, "onResponse() " + w.toString());
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
                if (sgObj.has("savedPercentage")) {
                    savingsGoal.setSavedPercentage(sgObj.getInt("savedPercentage"));
                }
                if (sgObj.has("target")) {
                    JSONObject sgTrgtObj = sgObj.getJSONObject("target");
                    CurrencyAndAmount target = new CurrencyAndAmount();
                    target.setCurrency(sgTrgtObj.getString("currency"));
                    target.setMinorUnits(sgTrgtObj.getInt("minorUnits"));
                    savingsGoal.setTarget(target);
                }

                JSONObject sgTotSvdObj = sgObj.getJSONObject("totalSaved");
                CurrencyAndAmount totalSaved = new CurrencyAndAmount();
                totalSaved.setCurrency(sgTotSvdObj.getString("currency"));
                totalSaved.setMinorUnits(sgTotSvdObj.getInt("minorUnits"));
                savingsGoal.setTotalSaved(totalSaved);

                savingsGoals.addSavingsGoal(savingsGoal);
            }
        } catch (Exception w) {
            Log.v(MainActivity.TAG, "onResponse() " + w.toString());
            //  Toast.makeText(MainActivity.this,w.getMessage(),Toast.LENGTH_LONG).show();
        }
        return savingsGoals;
    }

    private AccountBalance parseAccountBalance(JSONObject jsonObj) {
        AccountBalance acctBal = new AccountBalance();
        try {
            JSONObject intJsonObj = jsonObj.getJSONObject("clearedBalance");
            CurrencyAndAmount currAndAmt = new CurrencyAndAmount();
            currAndAmt.setCurrency(intJsonObj.getString("currency"));
            currAndAmt.setMinorUnits(intJsonObj.getInt("minorUnits"));
            acctBal.setClearedBalance(currAndAmt);

            intJsonObj = jsonObj.getJSONObject("effectiveBalance");
            currAndAmt = new CurrencyAndAmount();
            currAndAmt.setCurrency(intJsonObj.getString("currency"));
            currAndAmt.setMinorUnits(intJsonObj.getInt("minorUnits"));
            acctBal.setEffectiveBalance(currAndAmt);

            intJsonObj = jsonObj.getJSONObject("pendingTransactions");
            currAndAmt = new CurrencyAndAmount();
            currAndAmt.setCurrency(intJsonObj.getString("currency"));
            currAndAmt.setMinorUnits(intJsonObj.getInt("minorUnits"));
            acctBal.setPendingTransactions(currAndAmt);

            intJsonObj = jsonObj.getJSONObject("acceptedOverdraft");
            currAndAmt = new CurrencyAndAmount();
            currAndAmt.setCurrency(intJsonObj.getString("currency"));
            currAndAmt.setMinorUnits(intJsonObj.getInt("minorUnits"));
            acctBal.setAcceptedOverdraft(currAndAmt);

            intJsonObj = jsonObj.getJSONObject("amount");
            currAndAmt = new CurrencyAndAmount();
            currAndAmt.setCurrency(intJsonObj.getString("currency"));
            currAndAmt.setMinorUnits(intJsonObj.getInt("minorUnits"));
            acctBal.setAmount(currAndAmt);
        } catch (Exception w) {
            Log.v(MainActivity.TAG, "onResponse() " + w.toString());
            //  Toast.makeText(MainActivity.this,w.getMessage(),Toast.LENGTH_LONG).show();
        }
        return acctBal;
    }

    private void sendMoneyToSavingsGoal(Account account, SavingsGoal savingsGoal, int amountInPence) {
        Log.v(MainActivity.TAG, "addMoneyToSavingsGoal()");

        UUID transferUid = UUID.randomUUID();
        String url = this.apiDomainUrl + new Formatter().format(
                PennySavingsWorker.ADD_TO_SAVINGS_GOALS_URL,
                account.getAccountUid(),
                savingsGoal.getSavingsGoalUid(),
                transferUid
        );

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

        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                null,
                response -> {
                    Log.v(MainActivity.TAG, "onResponse() " + response.toString());
                    JSONObject jsonObj = response;
                    //   SavingsGoals savingsGoals = parseSavingsGoals(jsonObj);
                    readAccounts();
                }, error -> {
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

    public void createSavingsGoal(Account account, String savingsGoalName, CurrencyAndAmount targetAmount) {
        Log.v(MainActivity.TAG, "createSavingsGoal(" + savingsGoalName + ")");

        String url = this.apiDomainUrl + new Formatter().format(
                PennySavingsWorker.CREATE_SAVINGS_GOALS_URL,
                account.getAccountUid()
        );

        JSONObject jsonBody = new JSONObject();
        try {
            JSONObject jsonBodyTrgt = new JSONObject();
            jsonBodyTrgt.put("currency", targetAmount.getCurrency());
            jsonBodyTrgt.put("minorUnits", targetAmount.getMinorUnits());

            jsonBody.put("currency", targetAmount.getCurrency());
            jsonBody.put("name", savingsGoalName);
            jsonBody.put("target", jsonBodyTrgt);
        } catch (JSONException e) {
            Log.v(MainActivity.TAG, "addMoneyToSavingsGoal() creating body " + e.toString());
        }
        final String requestBody = jsonBody.toString();

        JsonObjectRequest jsonArrayRequest = new JsonObjectRequest(
                Request.Method.PUT,
                url,
                null,
                response -> {
                    Log.v(MainActivity.TAG, "onResponse() " + response.toString());
                    JSONObject jsonObj = response;
                    readSavingsGoals(account);
                }, error -> {
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
