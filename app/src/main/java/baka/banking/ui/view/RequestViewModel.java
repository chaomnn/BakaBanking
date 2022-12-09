package baka.banking.ui.view;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import baka.banking.model.RequestError;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.NotNull;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.List;

public abstract class RequestViewModel extends ViewModel {

    final MutableLiveData<RequestError> error = new MutableLiveData<>();
    final CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final OkHttpClient client = new OkHttpClient.Builder().cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(@NotNull HttpUrl httpUrl, @NotNull List<Cookie> list) {
                    list.forEach(c -> {
                        HttpCookie cookie = new HttpCookie(c.name(), c.value());
                        cookie.setDomain(c.domain());
                        cookieManager.getCookieStore().add(httpUrl.uri(), cookie);
                    });
                }

                @NotNull
                @Override
                public List<Cookie> loadForRequest(@NotNull HttpUrl httpUrl) {
                    List<Cookie> cookies = new ArrayList<>();
                    cookieManager.getCookieStore().getCookies().forEach(c -> {
                        Cookie cookie = new Cookie.Builder()
                                .name(c.getName())
                                .value(c.getValue())
                                .domain(c.getDomain())
                                .build();
                        cookies.add(cookie);
                    });
                    return cookies;
                }
            })
            .build();

    public LiveData<RequestError> getError() {
        return error;
    }
}
