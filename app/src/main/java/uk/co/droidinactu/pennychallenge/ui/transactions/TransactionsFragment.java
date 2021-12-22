package uk.co.droidinactu.pennychallenge.ui.transactions;

import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import org.fabiomsr.moneytextview.MoneyTextView;
import uk.co.droidinactu.pennychallenge.MainActivity;
import uk.co.droidinactu.pennychallenge.MyApplication;
import uk.co.droidinactu.pennychallenge.PennySavingsWorker;
import uk.co.droidinactu.pennychallenge.R;
import uk.co.droidinactu.pennychallenge.starling.Account;
import uk.co.droidinactu.pennychallenge.starling.AccountBalance;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoal;
import uk.co.droidinactu.pennychallenge.starling.Transaction;
import uk.co.droidinactu.pennychallenge.ui.dashboard.DashboardViewModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Locale;

/** */
public class TransactionsFragment extends Fragment implements View.OnClickListener {

  private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
  private static final int NOTIFICATION_ID = 0;

  private Account account;
  private AccountBalance accountBalance;
  private DashboardViewModel dashboardViewModel;
  private PennySavingsWorker pennySavingsWorker;
  private NotificationManager mNotifyManager;
  private Button btnFromDatePicker;
  private EditText txtFromDate;
  private Button btnToDatePicker;
  private EditText txtToDate;
  private Button btnRoundup;
  private LocalDateTime fromDate;
  private LocalDateTime toDate;
  private LinearLayout tLayout;
  private MoneyTextView txt_accountBalance;
  private TextView txt_accountName;
  private Spinner spn_savingsGoals;

  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

    View root = inflater.inflate(R.layout.fragment_transactions, container, false);

    tLayout = (LinearLayout) root.findViewById(R.id.layout_transactions);

    btnRoundup = (Button) root.findViewById(R.id.btn_roundUp);
    btnRoundup.setTag("0.0");
    btnFromDatePicker = (Button) root.findViewById(R.id.btn_fromDate);
    txtFromDate = (EditText) root.findViewById(R.id.from_date);

    btnToDatePicker = (Button) root.findViewById(R.id.btn_toDate);
    txtToDate = (EditText) root.findViewById(R.id.to_date);

    txt_accountName = root.findViewById(R.id.txt_accountName);
    txt_accountBalance = root.findViewById(R.id.txt_accountBalance);
    spn_savingsGoals = root.findViewById(R.id.spn_savingsGoals);

    btnFromDatePicker.setOnClickListener(this);
    btnToDatePicker.setOnClickListener(this);
    btnRoundup.setOnClickListener(this);

    fromDate = LocalDateTime.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
    txtFromDate.setText(
        fromDate.getDayOfMonth() + " " + fromDate.getMonth() + " " + fromDate.getYear());

    toDate = LocalDateTime.now();
    txtToDate.setText(toDate.getDayOfMonth() + " " + toDate.getMonth() + " " + toDate.getYear());

    setupDataRetrieval(inflater, container, root);
    return root;
  }

  private void setupDataRetrieval(LayoutInflater inflater, ViewGroup container, View root) {
    Log.v(MainActivity.TAG, "TransactionsFragment::setupDataRetrieval()");

    dashboardViewModel
        .getAccountBalance()
        .observe(
            getViewLifecycleOwner(),
            actBalance -> {
              if (actBalance != null) {
                accountBalance = actBalance;
                float amountInPence = accountBalance.getEffectiveBalance().getMinorUnits() / 100;
                txt_accountBalance.setAmount(amountInPence);
              }
              if (Float.parseFloat(btnRoundup.getTag().toString()) <= txt_accountBalance.getAmount()
                  && (spn_savingsGoals.getAdapter() != null
                      && spn_savingsGoals.getAdapter().getCount() > 0)) {
                btnRoundup.setEnabled(true);
              } else {
                btnRoundup.setText(getString(R.string.btn_roundup));
                btnRoundup.setEnabled(false);
              }
            });

    dashboardViewModel
        .getTransactions()
        .observe(
            getViewLifecycleOwner(),
            transactions -> {
              if (transactions != null) {
                int roundUpAmount = 0;
                tLayout.removeAllViews();
                btnRoundup.setText(getString(R.string.btn_roundup));
                Log.v(
                    MainActivity.TAG,
                    "TransactionsFragment::setupDataRetrieval() populate transactions list");
                for (Transaction t : transactions.getFeedItems()) {
                  View sgView = inflater.inflate(R.layout.transaction, container, false);
                  tLayout.addView(sgView);

                  final TextView txt_transactionDate =
                      sgView.findViewById(R.id.txt_transactionDate);
                  final MoneyTextView txt_transactionBalance =
                      sgView.findViewById(R.id.txt_transactionBalance);

                  roundUpAmount += 100 - (t.getAmount().getMinorUnits() % 100);

                  txt_transactionDate.setText(t.getUpdatedAt().toLocalDate().toString());
                  txt_transactionBalance.setAmount(t.getAmount().getMinorUnits() / 100);
                }
                BigDecimal roundupDecimal =
                    BigDecimal.valueOf(roundUpAmount)
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                btnRoundup.setText(
                    getString(R.string.btn_roundup) + roundupDecimal.toPlainString());
                btnRoundup.setTag(roundupDecimal);
                if ((roundUpAmount / 100) <= txt_accountBalance.getAmount()
                    && spn_savingsGoals.getAdapter() != null
                    && spn_savingsGoals.getAdapter().getCount() > 0) {
                  btnRoundup.setEnabled(true);
                } else {
                  btnRoundup.setText(getString(R.string.btn_roundup));
                  btnRoundup.setEnabled(false);
                }
              }
            });

    dashboardViewModel
        .getAccounts()
        .observe(
            getViewLifecycleOwner(),
            accts -> {
              if (accts != null) {
                for (Account a : accts.getAccounts()) {
                  account = a;
                  dashboardViewModel.loadAccountBalance(a);
                  dashboardViewModel.loadTransactions(a, fromDate, toDate);
                  dashboardViewModel.loadSavingsGoals(a);
                  txt_accountName.setText(a.getName() + " balance:");
                }
              }
            });

    dashboardViewModel
        .getSavingsGoals()
        .observe(
            getViewLifecycleOwner(),
            savingGoals -> {
              if (savingGoals != null) {
                ArrayList<String> savingsGoalNames = new ArrayList<>();
                savingGoals
                    .getSavingsGoals()
                    .forEach(
                        sg -> {
                          savingsGoalNames.add(sg.getName());
                        });
                ArrayAdapter<String> adapter =
                    new ArrayAdapter<String>(
                        this.getContext(), android.R.layout.simple_spinner_item, savingsGoalNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spn_savingsGoals.setAdapter(adapter);
              }
              if (Float.parseFloat(btnRoundup.getTag().toString()) <= txt_accountBalance.getAmount()
                  && spn_savingsGoals.getAdapter() != null
                  && spn_savingsGoals.getAdapter().getCount() > 0) {
                btnRoundup.setEnabled(true);
              } else {
                btnRoundup.setText(getString(R.string.btn_roundup));
                btnRoundup.setEnabled(false);
              }
            });
  }

  private void sendNotification(int amountSaved) {
    Currency ukCurrency = Currency.getInstance(Locale.UK);
    NumberFormat ukCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.UK);
    float amountFloat = amountSaved / 100.0f;
    String amountStr = ukCurrencyFormat.format(amountFloat);
    String notificationStr = String.format("You've saved another %s!", amountStr);

    NotificationCompat.Builder notifyBuilder =
        new NotificationCompat.Builder(getContext(), PRIMARY_CHANNEL_ID)
            .setContentTitle("Penny Savings Challenge")
            .setContentText(notificationStr)
            .setTimeoutAfter(600000)
            .setSmallIcon(R.drawable.ic_dashboard_black_24dp);
    mNotifyManager.notify(NOTIFICATION_ID, notifyBuilder.build());
  }

  public void createNotificationChannel() {
    mNotifyManager =
        (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
    // Create a NotificationChannel
    NotificationChannel notificationChannel =
        new NotificationChannel(
            PRIMARY_CHANNEL_ID, "Penny Savings Notification", NotificationManager.IMPORTANCE_HIGH);
    notificationChannel.enableLights(true);
    notificationChannel.setLightColor(Color.MAGENTA);
    notificationChannel.enableVibration(true);
    notificationChannel.setDescription("Notification from Mascot");
    mNotifyManager.createNotificationChannel(notificationChannel);
  }

  @Override
  public void onStart() {
    Log.v(MainActivity.TAG, "TransactionsFragment::onStart()");
    super.onStart();
    createNotificationChannel();
  }

  @Override
  public void onResume() {
    Log.v(MainActivity.TAG, "TransactionsFragment::onResume()");
    super.onResume();
  }

  /**
   * Called when a view has been clicked.
   *
   * @param v The view that was clicked.
   */
  @Override
  public void onClick(View v) {
    if (v == btnToDatePicker) {
      Log.d(MainActivity.TAG, "TransactionsFragment::onClick() - btnToDatePicker");

      DatePickerDialog datePickerDialog =
          new DatePickerDialog(
              this.getContext(),
              new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                  toDate = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0);
                  txtToDate.setText(
                      toDate.getDayOfMonth() + " " + toDate.getMonth() + " " + toDate.getYear());
                  if (fromDate.isBefore(toDate)) {
                    dashboardViewModel.loadTransactions(account, fromDate, toDate);
                  } else {
                    tLayout.removeAllViews();
                    btnRoundup.setText(getString(R.string.btn_roundup));
                    btnRoundup.setEnabled(false);
                  }
                }
              },
              toDate.getYear(),
              toDate.getMonthValue() - 1,
              toDate.getDayOfMonth());
      datePickerDialog.show();
    } else if (v == btnFromDatePicker) {
      Log.d(MainActivity.TAG, "TransactionsFragment::onClick() - btnFromDatePicker");
      DatePickerDialog datePickerDialog =
          new DatePickerDialog(
              this.getContext(),
              new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                  fromDate = LocalDateTime.of(year, monthOfYear + 1, dayOfMonth, 0, 0);
                  txtFromDate.setText(
                      fromDate.getDayOfMonth()
                          + " "
                          + fromDate.getMonth()
                          + " "
                          + fromDate.getYear());
                  if (fromDate.isBefore(toDate)) {
                    dashboardViewModel.loadTransactions(account, fromDate, toDate);
                  } else {
                    tLayout.removeAllViews();
                    btnRoundup.setText(getString(R.string.btn_roundup));
                    btnRoundup.setEnabled(false);
                  }
                }
              },
              fromDate.getYear(),
              fromDate.getMonthValue() - 1,
              fromDate.getDayOfMonth());
      datePickerDialog.show();
      datePickerDialog.show();
    } else if (v == btnRoundup) {
      Log.d(MainActivity.TAG, "TransactionsFragment::onClick() - btnRoundup");
      MyApplication myApp = (MyApplication) getActivity().getApplication();
      int amountToSave = (int) (Float.parseFloat(btnRoundup.getTag().toString()) * 100);
      SavingsGoal savingsGoal =
          dashboardViewModel.getSavingsGoal(spn_savingsGoals.getSelectedItem().toString());
      if (savingsGoal != null) {
        myApp
            .getThreadPoolExecutor()
            .execute(
                () ->
                    dashboardViewModel.addMoneyToSavingsGoal(
                        account, savingsGoal, accountBalance, amountToSave));
      }
    }
  }
}
