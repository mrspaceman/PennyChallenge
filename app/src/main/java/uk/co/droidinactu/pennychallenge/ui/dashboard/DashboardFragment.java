package uk.co.droidinactu.pennychallenge.ui.dashboard;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.room.Room;

import org.fabiomsr.moneytextview.MoneyTextView;

import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import uk.co.droidinactu.pennychallenge.starling.SavingsGoal;
import uk.co.droidinactu.pennychallenge.starling.SavingsGoals;

/**
 *
 */
public class DashboardFragment extends Fragment {

    private Account account;
    private AppDatabase db;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    private DashboardViewModel dashboardViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);

        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);
        final TextView txt_accountName = root.findViewById(R.id.txt_accountName);
        final MoneyTextView txt_accountBalance = root.findViewById(R.id.txt_accountBalance);

        db = Room.databaseBuilder(getActivity().getApplicationContext(),
                AppDatabase.class, "saved-to-date").build();

        dashboardViewModel.getSavingsGoals().observe(getViewLifecycleOwner(), new Observer<SavingsGoals>() {
            @Override
            public void onChanged(@Nullable SavingsGoals savingGoals) {
                if (savingGoals != null) {
                    final LinearLayout sgLayout = root.findViewById(R.id.layout_savingsGoals);
                    sgLayout.removeAllViews();
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

                        if (sgFinal.getName().contains("enny")) {
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
                }
            }
        });

        dashboardViewModel.getAccountBalance().observe(getViewLifecycleOwner(), new Observer<AccountBalance>() {
            @Override
            public void onChanged(@Nullable AccountBalance actBalance) {
                if (actBalance != null) {
                    float amountInPence = actBalance.getAmount().getMinorUnits() / 100;
                    txt_accountBalance.setAmount(amountInPence);
                }
            }
        });

        dashboardViewModel.getAccounts().observe(getViewLifecycleOwner(), new Observer<Accounts>() {
            @Override
            public void onChanged(@Nullable Accounts accts) {
                if (accts != null) {
                    for (Account a : accts.getAccounts()) {
                        // FIXME : do something with accounts
                        account = a;
                        dashboardViewModel.loadAccountBalance(a);
                        dashboardViewModel.loadSavingsGoals(a);
                        txt_accountName.setText(a.getName());
                    }
                }
            }
        });

        updateDayProgress(root);
        return root;
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
            dashboardViewModel.addMoneyToSavingsGoal(account, savingsGoal, amountToSave);
        } else {
            Log.v(MainActivity.TAG, "DashboardFragment::makePennyPayments() penny savings up to date");
        }
    }

    @Override
    public void onStart() {
        Log.v(MainActivity.TAG, "DashboardFragment::onStart()");
        super.onStart();
    }

    @Override
    public void onResume() {
        Log.v(MainActivity.TAG, "DashboardFragment::onResume()");
        super.onResume();
    }

}