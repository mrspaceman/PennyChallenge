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
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.Room;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import pl.pawelkleczkowski.customgauge.CustomGauge;
import uk.co.droidinactu.pennychallenge.MainActivity;
import uk.co.droidinactu.pennychallenge.MyApplication;
import uk.co.droidinactu.pennychallenge.R;
import uk.co.droidinactu.pennychallenge.database.AppDatabase;
import uk.co.droidinactu.pennychallenge.database.SavedOnDate;
import uk.co.droidinactu.pennychallenge.database.SavedOnDateDao;
import uk.co.droidinactu.pennychallenge.starling.Account;
import uk.co.droidinactu.pennychallenge.starling.AccountBalance;
import uk.co.droidinactu.pennychallenge.starling.Accounts;
import uk.co.droidinactu.pennychallenge.starling.CurrencyAndAmount;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoal;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoals;

/**
 *
 */
public class DashboardFragment extends Fragment {

    public static final String PENNY_CHALLENGE = "PennyChallenge";

    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    private static final int NOTIFICATION_ID = 0;

    private Account account;
    private AccountBalance accountBalance;
    private AppDatabase db;
    private DashboardViewModel dashboardViewModel;

    private NotificationManager mNotifyManager;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        db = Room.databaseBuilder(getActivity().getApplicationContext(),
                AppDatabase.class, "saved-to-date").build();
        setupDataRetrival(inflater, container, root);
        updateDayProgress(root);
        return root;
    }

    private void setupDataRetrival(LayoutInflater inflater, ViewGroup container, View root) {
        final TextView txt_accountName = root.findViewById(R.id.txt_accountName);
        final MoneyTextView txt_accountBalance = root.findViewById(R.id.txt_accountBalance);

        dashboardViewModel.getSavingsGoals().observe(getViewLifecycleOwner(), new Observer<SavingsGoals>() {
            @Override
            public void onChanged(@Nullable SavingsGoals savingGoals) {
                if (savingGoals != null) {
                    final LinearLayout sgLayout = root.findViewById(R.id.layout_savingsGoals);
                    sgLayout.removeAllViews();
                    boolean pennyChallengeFound = false;
                    for (SavingsGoal s : savingGoals.getSavingsGoals()) {
                        View sgView = inflater.inflate(R.layout.savings_goal, container, false);
                        sgLayout.addView(sgView);

                        final TextView txt_savingsGoalName = sgView.findViewById(R.id.txt_savingsGoalName);
                        final MoneyTextView txt_savingsGoalTarget = sgView.findViewById(R.id.txt_savingsGoalTarget);
                        final MoneyTextView txt_savingsGoalBalance = sgView.findViewById(R.id.txt_savingsGoalBalance);
                        final TextView txt_savingsGoalPercentage = sgView.findViewById(R.id.txt_savingsGoalPercentage);

                        final SavingsGoal sgFinal = s;
                        txt_savingsGoalName.setText(sgFinal.getName());

                        float targetAmount = sgFinal.getTarget().getMinorUnits() / 100;
                        txt_savingsGoalTarget.setAmount(targetAmount);

                        float amountInPence = sgFinal.getTotalSaved().getMinorUnits() / 100;
                        txt_savingsGoalBalance.setAmount(amountInPence);

                        int savedPercent = sgFinal.getSavedPercentage();
                        String percentString = getString(R.string.txt_savingsGoal_savedPercent, savedPercent);
                        txt_savingsGoalPercentage.setText(percentString);

                        if (sgFinal.getName().contains(PENNY_CHALLENGE)) {
                            pennyChallengeFound = true;
                            MyApplication myApp = (MyApplication) getActivity().getApplication();
                            myApp.getThreadPoolExecutor()
                                    .execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            makePennyPayments(sgFinal);
                                        }
                                    });
                        }
                    }
                    if (!pennyChallengeFound) {
                        CurrencyAndAmount newTargetAmount = new CurrencyAndAmount();
                        newTargetAmount.setCurrency(account.getCurrency());
                        newTargetAmount.setMinorUnits(65000);
                        dashboardViewModel.createSavingsGoal(account, PENNY_CHALLENGE, newTargetAmount);
                    }
                }
            }
        });

        dashboardViewModel.getAccountBalance().observe(getViewLifecycleOwner(), new Observer<AccountBalance>() {
            @Override
            public void onChanged(@Nullable AccountBalance actBalance) {
                if (actBalance != null) {
                    accountBalance = actBalance;
                    float amountInPence = accountBalance.getEffectiveBalance().getMinorUnits() / 100;
                    txt_accountBalance.setAmount(amountInPence);
                }
            }
        });

        dashboardViewModel.getAccounts().observe(getViewLifecycleOwner(), new Observer<Accounts>() {
            @Override
            public void onChanged(@Nullable Accounts accts) {
                if (accts != null) {
                    for (Account a : accts.getAccounts()) {
                        account = a;
                        dashboardViewModel.loadAccountBalance(a);
                        dashboardViewModel.loadSavingsGoals(a);
                        txt_accountName.setText(a.getName());
                    }
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

    private void makePennyPayments(SavingsGoal savingsGoal) {
        Log.v(MainActivity.TAG, "DashboardFragment::makePennyPayments()");

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
            Log.v(MainActivity.TAG, "DashboardFragment::makePennyPayments() saving [" + amountToSave + "] pence");
            if (accountBalance.getEffectiveBalance().getMinorUnits() < amountToSave) {
                Log.e(MainActivity.TAG, "DashboardFragment::makePennyPayments() penny savings up to date");
            } else {
                dashboardViewModel.addMoneyToSavingsGoal(account, savingsGoal, amountToSave);
                sendNotification(amountToSave);
            }
        } else {
            Log.v(MainActivity.TAG, "DashboardFragment::makePennyPayments() penny savings up to date");
        }
    }

    private void sendNotification(int amountSaved) {
        Currency ukCurrency = Currency.getInstance(Locale.UK);
        NumberFormat ukCurrencyFormat = NumberFormat.getCurrencyInstance(Locale.UK);
        float amountFloat = Float.valueOf(amountSaved / 100.0f);
        String amountStr = ukCurrencyFormat.format(amountFloat);
        String notificationStr = String.format("You've saved another %s!", amountStr);

        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(getContext(), PRIMARY_CHANNEL_ID)
                .setContentTitle("Penny Savings Challenge")
                .setContentText(notificationStr)
                .setTimeoutAfter(600000)
                .setSmallIcon(R.drawable.ic_dashboard_black_24dp);
        mNotifyManager.notify(NOTIFICATION_ID, notifyBuilder.build());
    }


    public void createNotificationChannel() {
        mNotifyManager = (NotificationManager) getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >=
                android.os.Build.VERSION_CODES.O) {
            // Create a NotificationChannel
            NotificationChannel notificationChannel = new NotificationChannel(PRIMARY_CHANNEL_ID,
                    "Penny Savings Notification", NotificationManager
                    .IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.MAGENTA);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Mascot");
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
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