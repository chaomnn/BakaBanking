package baka.banking.ui.view;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import baka.banking.model.RequestError;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;

public class LoginViewModel extends RequestViewModel {

    private static final String URL_CAPTCHA = "https://www.ktbnetbank.com/consumer/captcha/verifyImg";
    private static final String URL_LOGIN ="https://www.ktbnetbank.com/consumer/Login.do ";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private final MutableLiveData<RequestError> error = new MutableLiveData<>();
    private final MutableLiveData<HashMap<String, String>> cookies = new MutableLiveData<>();
    private final MutableLiveData<Bitmap> captchaImage = new MutableLiveData<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SavedStateHandle state;

    public LoginViewModel(SavedStateHandle savedStateHandle) {
        super();
        state = savedStateHandle;
    }

    public LiveData<HashMap<String, String>> getCookies(String username, String password, String captcha) {
        if (cookies.getValue() == null) {
            authenticate(username, password, captcha);
        }
        return cookies;
    }

    public LiveData<Bitmap> getCaptchaImage() {
        getCaptcha();
        return captchaImage;
    }

    private void authenticate(String username, String password, String captcha) {
        RequestBody formBody = new FormBody.Builder()
                .add("cmd", "login")
                .add("userId", username)
                .add("password", password)
                .add("imageCode", captcha)
                .build();
        client.newCall(new Request.Builder()
                .url(URL_LOGIN)
                .addHeader("Content-Type", CONTENT_TYPE)
                .post(formBody)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                handler.post(() -> error.setValue(RequestError.LOGIN_ERROR));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    HashMap<String, String> map = new HashMap<>();
                    cookieManager.getCookieStore().getCookies().forEach(c -> map.put(c.getName(), c.getValue()));
                    handler.post(() -> cookies.setValue(map));
                }
            }
        });
    }

    private void getCaptcha() {
        client.newCall(new Request.Builder().url(URL_CAPTCHA).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                handler.post(() -> error.setValue(RequestError.CAPTCHA_ERROR));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    handler.post(() -> captchaImage.setValue(BitmapFactory.decodeStream(response.body().byteStream())));
                } else {
                    handler.post(() -> error.setValue(RequestError.CAPTCHA_ERROR));
                }
            }
        });
    }
}
