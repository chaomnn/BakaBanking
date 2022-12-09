package baka.banking.model;

import android.icu.text.UFormat;
import android.os.Parcel;
import android.os.Parcelable;
import org.jetbrains.annotations.NotNull;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class Transaction implements Parcelable {

    private String date;
    private int type;
    private String description;
    private String amount;
    private String remainingBalance;

    private static final String WEB_PREFIX = "WWW.";
    private static final String DATE_FORMAT = "dd-MM-yyyy HH:mm:ss";
    private static final SimpleDateFormat FORMAT = new SimpleDateFormat(DATE_FORMAT, Locale.ROOT);

    public Transaction(ArrayList<String> data) {
        this.date = data.get(0);
        this.type = findByName(data.get(1));
        this.description = parseDescription(data.get(2));
        this.amount = data.get(3);
        this.remainingBalance = data.get(4);
    }

    protected Transaction(Parcel in) {
        date = in.readString();
        type = in.readInt();
        description = in.readString();
        amount = in.readString();
        remainingBalance = in.readString();
    }

    public static final Creator<Transaction> CREATOR = new Creator<Transaction>() {
        @Override
        public Transaction createFromParcel(Parcel in) {
            return new Transaction(in);
        }

        @Override
        public Transaction[] newArray(int size) {
            return new Transaction[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(date);
        dest.writeInt(type);
        dest.writeString(description);
        dest.writeString(amount);
        dest.writeString(remainingBalance);
    }

    @NotNull
    @Override
    public String toString() {
        return description + "  " + amount;
    }

    private String parseDescription(String description) {
        if (description.startsWith(WEB_PREFIX)) {
            return description.substring(WEB_PREFIX.length(), description.indexOf(".", WEB_PREFIX.length()));
        }
        return description;
    }

    public int getType() {
        return type;
    }

    public long getDate() {
        try {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(Objects.requireNonNull(FORMAT.parse(date)));
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            return calendar.getTimeInMillis();
        } catch (NullPointerException | ParseException e) {
            e.printStackTrace();
            return 0L;
        }
    }

    public String getRemainingBalance() {
        return remainingBalance;
    }

    public enum TransactionType {
        ONLINE_PURCHASE("LSSEC"),
        OFFLINE_PURCHASE("LSSWP"),
        DEPOSIT("ATSDC"),
        WITHDRAWAL("ATSWC"),
        TRANSFER("XISDT"),
        UNKNOWN_TRANSACTION("");

        private String name;

        TransactionType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private int findByName(String name) {
        for (TransactionType type : TransactionType.values()) {
            if (type.name.equals(name)) {
                return type.ordinal();
            }
        }
        return TransactionType.UNKNOWN_TRANSACTION.ordinal();
    }
}
