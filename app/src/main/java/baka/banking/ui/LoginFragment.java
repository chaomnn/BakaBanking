package baka.banking.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import baka.banking.R;
import baka.banking.databinding.FragmentLoginBinding;
import baka.banking.ui.view.LoginViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;

public class LoginFragment extends Fragment {

    public interface LoginCallback {
        void authenticate(HashMap<String, String> loginCookies, String dateFrom, String dateTo);
    }

    private LoginCallback getCallback() {
        return (LoginCallback) requireContext();
    }

    private static final String KEY_ERROR_MESSAGE = "errorMessage";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";

    private FragmentLoginBinding binding;
    private HashMap<String, String> loginCookies;

    public LoginFragment() {
        super(R.layout.fragment_login);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentLoginBinding.bind(view);
        final LoginViewModel loginViewModel = new ViewModelProvider(this).get(LoginViewModel.class);
        loginViewModel.getCaptchaImage().observe(this, bitmap -> binding.captchaImage.setImageBitmap(bitmap));
        try {
            if (requireArguments().getString(KEY_ERROR_MESSAGE) != null) {
                Snackbar.make(binding.getRoot(),
                        requireArguments().getString(KEY_ERROR_MESSAGE), Snackbar.LENGTH_SHORT).show();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
        binding.reloadCaptcha.setOnClickListener(v ->
                loginViewModel.getCaptchaImage().observe(this, bitmap -> binding.captchaImage.setImageBitmap(bitmap)));

        binding.checkbox.setOnClickListener(v -> {
            getPreferences().edit().putString(KEY_USERNAME, binding.username.getText().toString()).apply();
            getPreferences().edit().putString(KEY_PASSWORD, binding.password.getText().toString()).apply();
        });

        if (!getPreferences().getString(KEY_USERNAME, "").isEmpty() && !getPreferences().getString(KEY_PASSWORD, "").isEmpty()) {
            binding.username.setVisibility(View.GONE);
            binding.password.setVisibility(View.GONE);
            binding.logoutButton.setVisibility(View.VISIBLE);
            binding.checkbox.setVisibility(View.GONE);
        }
        binding.login.setOnClickListener(v -> loginViewModel.getCookies(
                binding.username.getVisibility() != View.GONE ?
                binding.username.getText().toString() : getPreferences().getString(KEY_USERNAME, ""),
                        binding.password.getVisibility() != View.GONE ?
                        binding.password.getText().toString() : getPreferences().getString(KEY_PASSWORD, ""),
                        binding.captcha.getText().toString())
                .observe(this, cookies -> {loginCookies = cookies;
                    getCallback().authenticate(loginCookies,
                            binding.dateFrom.getText().toString(),
                            binding.dateTo.getText().toString());
                }));
        binding.logoutButton.setOnClickListener(v -> {
            getPreferences().edit().remove(KEY_USERNAME).apply();
            getPreferences().edit().remove(KEY_PASSWORD).apply();
            binding.username.setVisibility(View.VISIBLE);
            binding.password.setVisibility(View.VISIBLE);
            binding.checkbox.setVisibility(View.VISIBLE);
            binding.logoutButton.setVisibility(View.GONE);
        });
        loginViewModel.getError().observe(this, error -> {
            String errorMessage;
            switch (error) {
                case CAPTCHA_ERROR:
                    errorMessage = getString(R.string.captcha_failed);
                    break;
                case LOGIN_ERROR:
                default:
                    errorMessage = getString(R.string.login_failed);
                    break;
            }
            Snackbar.make(binding.getRoot(), errorMessage, Snackbar.LENGTH_SHORT).show();
        });
    }

    private SharedPreferences getPreferences() {
        return getContext().getSharedPreferences(getContext().getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }
}
