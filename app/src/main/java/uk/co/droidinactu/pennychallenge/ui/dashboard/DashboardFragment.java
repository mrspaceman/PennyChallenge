package uk.co.droidinactu.pennychallenge.ui.dashboard;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.Room;
import org.fabiomsr.moneytextview.MoneyTextView;
import pl.pawelkleczkowski.customgauge.CustomGauge;
import uk.co.droidinactu.pennychallenge.MainActivity;
import uk.co.droidinactu.pennychallenge.MyApplication;
import uk.co.droidinactu.pennychallenge.PennySavingsWorker;
import uk.co.droidinactu.pennychallenge.R;
import uk.co.droidinactu.pennychallenge.database.AppDatabase;
import uk.co.droidinactu.pennychallenge.starling.Account;
import uk.co.droidinactu.pennychallenge.starling.AccountBalance;
import uk.co.droidinactu.pennychallenge.starling.CurrencyAndAmount;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoal;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.Locale;

/** */
public class DashboardFragment extends Fragment {

  private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
  private static final int NOTIFICATION_ID = 0;

  private Account account;
  private AccountBalance accountBalance;
  private AppDatabase db;
  private DashboardViewModel dashboardViewModel;
  private PennySavingsWorker pennySavingsWorker;
  private NotificationManager mNotifyManager;

  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    dashboardViewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

    View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

    db =
        Room.databaseBuilder(
                getActivity().getApplicationContext(), AppDatabase.class, "saved-to-date")
            .build();
    setupDataRetrieval(inflater, container, root);
    updateDayProgress(root);
    return root;
  }

  private void setupDataRetrieval(LayoutInflater inflater, ViewGroup container, View root) {
    Log.v(MainActivity.TAG, "DashboardFragment::setupDataRetrieval()");
    final TextView txt_accountName = root.findViewById(R.id.txt_accountName);
    final MoneyTextView txt_accountBalance = root.findViewById(R.id.txt_accountBalance);

    dashboardViewModel
        .getSavingsGoals()
        .observe(
            getViewLifecycleOwner(),
            savingGoals -> {
              if (savingGoals != null) {
                final LinearLayout sgLayout = root.findViewById(R.id.layout_savingsGoals);
                sgLayout.removeAllViews();
                boolean pennyChallengeFound = false;
                float totalBalance = 0;
                for (SavingsGoal s : savingGoals.getSavingsGoals()) {
                  View sgView = inflater.inflate(R.layout.savings_goal, container, false);
                  sgLayout.addView(sgView);

                  final TextView txt_savingsGoalName =
                      sgView.findViewById(R.id.txt_savingsGoalName);
                  final TextView txt_savingsGoalPercentage =
                      sgView.findViewById(R.id.txt_savingsGoalPercentage);
                  final MoneyTextView txt_savingsGoalTarget =
                      sgView.findViewById(R.id.txt_savingsGoalTarget);
                  final MoneyTextView txt_savingsGoalBalance =
                      sgView.findViewById(R.id.txt_savingsGoalBalance);

                  final SavingsGoal sgFinal = s;
                  txt_savingsGoalName.setText(sgFinal.getName());

                  if (sgFinal.getTarget() != null) {
                    float targetAmount = sgFinal.getTarget().getMinorUnits() / 100;
                    txt_savingsGoalTarget.setAmount(targetAmount);
                  }
                  float amountInPence = sgFinal.getTotalSaved().getMinorUnits() / 100;
                  txt_savingsGoalBalance.setAmount(amountInPence);

                  int savedPercent = sgFinal.getSavedPercentage();
                  String percentString =
                      getString(R.string.txt_savingsGoal_savedPercent, savedPercent);
                  txt_savingsGoalPercentage.setText(percentString);

                  totalBalance += amountInPence;

                  if (sgFinal.getName().contains(PennySavingsWorker.PENNY_CHALLENGE)) {
                    pennyChallengeFound = true;
                    MyApplication myApp = (MyApplication) getActivity().getApplication();
                    myApp
                        .getThreadPoolExecutor()
                        .execute(
                            () ->
                                dashboardViewModel.addMoneyToSavingsGoal(
                                    account, sgFinal, accountBalance, db.savedOnDateDao()));
                  }
                }
                View balTotal = inflater.inflate(R.layout.balance_total, container, false);
                final MoneyTextView txt_balanceTotalAmount =
                    balTotal.findViewById(R.id.txt_balanceTotalAmount);
                txt_balanceTotalAmount.setAmount(
                    totalBalance + (accountBalance.getEffectiveBalance().getMinorUnits() / 100));
                sgLayout.addView(balTotal);
                if (!pennyChallengeFound) {
                  Log.v(
                      MainActivity.TAG,
                      "DashboardFragment::setupDataRetrieval() pennyChallengeFound is false: creating new savings goal");
                  CurrencyAndAmount newTargetAmount = new CurrencyAndAmount();
                  newTargetAmount.setCurrency(account.getCurrency());
                  newTargetAmount.setMinorUnits(65000);
                  //          dashboardViewModel.createSavingsGoal(account,
                  // PennySavingsWorker.PENNY_CHALLENGE, newTargetAmount);
                }
              }
            });

    dashboardViewModel
        .getAccountBalance()
        .observe(
            getViewLifecycleOwner(),
            actBalance -> {
              if (actBalance != null) {
                accountBalance = actBalance;
                float amountInPence = accountBalance.getEffectiveBalance().getMinorUnits() / 100;
                txt_accountBalance.setAmount(amountInPence);
                dashboardViewModel.loadSavingsGoals(
                    dashboardViewModel.getAccounts().getValue().getAccounts().get(0));
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
                  txt_accountName.setText(a.getName());
                }
              }
            });
  }

  /**
   * uses a guage from : https://github.com/pkleczko/CustomGauge
   *
   * @param rootView
   */
  private void updateDayProgress(View rootView) {
    Log.v(MainActivity.TAG, "DashboardFragment::updateDayGrid()");

    Calendar cal = Calendar.getInstance();
    int dayNumber = cal.get(Calendar.DAY_OF_YEAR);

    getString(R.string.day_x_of_365);
    final CustomGauge progressGuage = rootView.findViewById(R.id.dayProgressGuage);
    final TextView txt_dayProgressGuage = rootView.findViewById(R.id.txt_dayProgressGuage);
    progressGuage.setValue(dayNumber);

    txt_dayProgressGuage.setText(getString(R.string.day_x_of_365, String.valueOf(dayNumber)));
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
    Log.v(MainActivity.TAG, "DashboardFragment::onStart()");
    super.onStart();
    createNotificationChannel();
  }

  @Override
  public void onResume() {
    Log.v(MainActivity.TAG, "DashboardFragment::onResume()");
    super.onResume();
  }
}
