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
import uk.co.droidinactu.pennychallenge.PennySavingsWorker;
import uk.co.droidinactu.pennychallenge.R;
import uk.co.droidinactu.pennychallenge.starling.Account;
import uk.co.droidinactu.pennychallenge.starling.AccountBalance;
import uk.co.droidinactu.pennychallenge.starling.Transaction;
import uk.co.droidinactu.pennychallenge.ui.dashboard.DashboardViewModel;

import java.text.NumberFormat;
import java.time.LocalDate;
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

  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

    View root = inflater.inflate(R.layout.fragment_transactions, container, false);

    btnFromDatePicker = (Button) root.findViewById(R.id.btn_fromDate);
    txtFromDate = (EditText) root.findViewById(R.id.from_date);

    btnToDatePicker = (Button) root.findViewById(R.id.btn_toDate);
    txtToDate = (EditText) root.findViewById(R.id.to_date);

    btnFromDatePicker.setOnClickListener(this);
    btnToDatePicker.setOnClickListener(this);

    LocalDate lastWeek = LocalDate.now().minus(7, java.time.temporal.ChronoUnit.DAYS);
    txtFromDate.setText(
        lastWeek.getDayOfMonth() + " " + lastWeek.getMonth() + " " + lastWeek.getYear());

    LocalDate now = LocalDate.now();
    txtToDate.setText(now.getDayOfMonth() + " " + now.getMonth() + " " + now.getYear());

    setupDataRetrieval(inflater, container, root);
    return root;
  }

  private void setupDataRetrieval(LayoutInflater inflater, ViewGroup container, View root) {
    Log.v(MainActivity.TAG, "TransactionsFragment::setupDataRetrieval()");
    final TextView txt_accountName = root.findViewById(R.id.txt_accountName);
    final MoneyTextView txt_accountBalance = root.findViewById(R.id.txt_accountBalance);

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
            });

    dashboardViewModel
        .getTransactions()
        .observe(
            getViewLifecycleOwner(),
            transactions -> {
              if (transactions != null) {
                final LinearLayout tLayout = root.findViewById(R.id.layout_transactions);
                tLayout.removeAllViews();
                Log.v(
                    MainActivity.TAG,
                    "TransactionsFragment::setupDataRetrieval() - transactions.size() = ");
                for (Transaction t : transactions.getFeedItems()) {
                  View sgView = inflater.inflate(R.layout.transaction, container, false);
                  tLayout.addView(sgView);

                  final TextView txt_transactionDate =
                      sgView.findViewById(R.id.txt_transactionDate);
                  final MoneyTextView txt_transactionBalance =
                      sgView.findViewById(R.id.txt_transactionBalance);

                  txt_transactionDate.setText(t.getUpdatedAt().toString());
                  txt_transactionBalance.setAmount(t.getAmount().getMinorUnits() / 100);
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
                  dashboardViewModel.loadTransactions(a);
                  txt_accountName.setText(a.getName());
                }
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
    Log.v(MainActivity.TAG, "TransactionsFragment::onClick()");

    if (v == btnToDatePicker) {
      LocalDate now = LocalDate.now();

      DatePickerDialog datePickerDialog =
          new DatePickerDialog(
              this.getContext(),
              new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                  txtFromDate.setText(dayOfMonth + "-" + (monthOfYear + 1) + "-" + year);
                }
              },
              now.getYear(),
              now.getMonthValue(),
              now.getDayOfMonth());
      datePickerDialog.show();
    }
  }
}
