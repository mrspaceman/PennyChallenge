package uk.co.droidinactu.pennychallenge.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import uk.co.droidinactu.pennychallenge.R;

public class SettingsFragment extends Fragment {

  private SettingsViewModel settingsViewModel;

  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    settingsViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
    View root = inflater.inflate(R.layout.fragment_settings, container, false);
    final TextView textView = root.findViewById(R.id.txt_settings_productionApi);
    settingsViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
    return root;
  }
}
