package baka.banking.ui.view;

import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import baka.banking.model.RequestError;
import baka.banking.model.Transaction;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.HttpCookie;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class StatementViewModel extends RequestViewModel {

    private static final String URL_DETAILS = "https://www.ktbnetbank.com/consumer/SavingAccount.do?cmd=showDetails";
    private static final String URL_DOWNLOAD = "https://www.ktbnetbank.com/consumer/SavingAccount.do?cmd=download";
    private static final String URL_STATEMENT = "https://www.ktbnetbank.com/consumer/DownloadService.do?fileType=word";
    private static final String URL_GET_ACCOUNT_NUMBER = "https://www.ktbnetbank.com/consumer/SavingAccount.do?cmd=init";
    private static final String COOKIE_DOMAIN = "www.ktbnetbank.com";
    private static final String KEY_ACCOUNT_NO = "OID";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String FORM_ACCOUNT_NO = "accountNo";
    private static final int ACCOUNT_NO_LENGTH = 10;
    private static final int DATA_SIZE = 5;

    private final MutableLiveData<ArrayList<Transaction>> transactions = new MutableLiveData<>();
    private String accountNumber;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public void setCookies(HashMap<String, String> cookies) {
        cookies.forEach((name, value) -> {
            HttpCookie cookie = new HttpCookie(name, value);
            cookie.setDomain(COOKIE_DOMAIN);
            cookieManager.getCookieStore().add(null, cookie);
        });
    }

    public LiveData<ArrayList<Transaction>> getTransactions(String dateFrom, String dateTo) {
        if (transactions.getValue() == null) {
            loadTransactions(dateFrom, dateTo);
        }
        return transactions;
    }

    private void loadTransactions(String dateFrom, String dateTo) {
        final ArrayList<Transaction> transactionsList = new ArrayList<>();
        client.newCall(new Request.Builder()
                .url(URL_GET_ACCOUNT_NUMBER)
                .build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                handler.post(() -> error.setValue(RequestError.ACCOUNT_NUMBER_ERROR));
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String responseBody = Objects.requireNonNull(response.body()).string();
                int index = responseBody.indexOf(KEY_ACCOUNT_NO) + KEY_ACCOUNT_NO.length() + 1;
                try {
                    Integer.parseInt(responseBody.substring(index, index + ACCOUNT_NO_LENGTH - 1));
                    accountNumber = responseBody.substring(index, index + ACCOUNT_NO_LENGTH);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    handler.post(() -> error.setValue(RequestError.ACCOUNT_NUMBER_ERROR));
                    return;
                }
                try (Response postDetails = client.newCall(buildRequest(URL_DETAILS,
                                new FormBody.Builder()
                                        .add(FORM_ACCOUNT_NO, String.valueOf(accountNumber))
                                        .build()))
                        .execute()) {
                } catch (IOException e) {
                    e.printStackTrace();
                    handler.post(() -> error.setValue(RequestError.ACCOUNT_NUMBER_ERROR));
                }
                RequestBody formBody = new FormBody.Builder()
                        .add("from_date", dateFrom)
                        .add("to_date", dateTo)
                        .add("radios", "date_peroid")
                        .add("specific_peroid", "currentMonth")
                        .add(FORM_ACCOUNT_NO, String.valueOf(accountNumber))
                        .build();
                try (Response nextResponse = client.newCall(buildRequest(URL_DOWNLOAD, formBody)).execute()) {
                    if (nextResponse.isSuccessful()) {
                        StringBuilder responseDocument = new StringBuilder();
                        try (Response downloadResponse = client.newCall(new Request.Builder()
                                .url(URL_STATEMENT)
                                .build()).execute()) {
                            responseDocument.append(Objects.requireNonNull(downloadResponse.body()).string());
                        }
                        Element table = Jsoup.parse(responseDocument.toString()).selectFirst("table");
                        Objects.requireNonNull(table).select("tr").forEach(row -> {
                            ArrayList<String> transactionBuilder = new ArrayList<>();
                            row.select("td").forEach(element -> {
                                if (!element.text().isEmpty()) {
                                    transactionBuilder.add(element.text());
                                }
                            });
                            if (transactionBuilder.size() == DATA_SIZE) {
                                Transaction transaction = new Transaction(transactionBuilder);
                                transactionsList.add(transaction);
                            }
                        });
                        handler.post(() -> transactions.setValue(transactionsList));
                    } else {
                        handler.post(() -> error.setValue(RequestError.DOWNLOAD_ERROR));
                    }
                }
            }
        });
    }

    private Request buildRequest(String url, RequestBody formBody) {
        return new Request.Builder()
                .url(url)
                .addHeader("Content-Type", CONTENT_TYPE)
                .post(formBody)
                .build();
    }
}
