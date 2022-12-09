package baka.banking.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import baka.banking.databinding.ActivityLoginBinding;

import java.util.ArrayList;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity implements LoginFragment.LoginCallback,
        StatementFragment.StatementCallback {

    private static final String KEY_COOKIES = "cookies";
    private static final String KEY_DATE_FROM = "dateFrom";
    private static final String KEY_DATE_TO = "dateTo";
    private static final String KEY_ERROR_MESSAGE = "errorMessage";

    private ActivityLoginBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(binding.mainFragment.getId(), LoginFragment.class, null)
                    .commitNow();
        }
    }

    @Override
    public void authenticate(HashMap<String, String> loginCookies, String dateFrom, String dateTo) {
        Bundle args = new Bundle();
        ArrayList<String> list = new ArrayList<>();
        loginCookies.forEach((name, value) -> {
            list.add(name);
            list.add(value);
        });
        args.putStringArrayList(KEY_COOKIES, list);
        args.putString(KEY_DATE_FROM, dateFrom);
        args.putString(KEY_DATE_TO, dateTo);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.mainFragment.getId(), StatementFragment.class, args)
                .commitNow();
    }

    @Override
    public void returnLogin(String errorMessage) {
        Bundle args = new Bundle();
        args.putString(KEY_ERROR_MESSAGE, errorMessage);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(binding.mainFragment.getId(), LoginFragment.class, args)
                .commitNow();
    }
}
