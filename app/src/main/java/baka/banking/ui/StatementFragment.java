package baka.banking.ui;

import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import baka.banking.R;
import baka.banking.databinding.FragmentStatementBinding;
import baka.banking.databinding.TransactionRowItemBinding;
import baka.banking.model.Transaction;
import baka.banking.ui.graphics.TransactionChart;
import baka.banking.ui.view.StatementViewModel;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

public class StatementFragment extends Fragment {

    private static final String KEY_COOKIES = "cookies";
    private static final String KEY_DATE_FROM = "dateFrom";
    private static final String KEY_DATE_TO = "dateTo";

    private static final String DATE_FORMAT_NEW = "d MMM yyyy, EEE";

    private FragmentStatementBinding binding;
    private final HashMap<String, String> cookies = new HashMap<>();
    private final HashMap<Integer, Integer> transactionsAmounts =
            new HashMap<>(Transaction.TransactionType.values().length);
    private String dateFrom, dateTo;

    public interface StatementCallback {
        void returnLogin(String errorMessage);
    }

    public StatementCallback getCallback() {
        return (StatementCallback) requireContext();
    }

    public StatementFragment() {
        super(R.layout.fragment_statement);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding = FragmentStatementBinding.bind(view);
        final Bundle arguments = requireArguments();
        final ArrayList<String> list = arguments.getStringArrayList(KEY_COOKIES);
        dateFrom = arguments.getString(KEY_DATE_FROM);
        dateTo = arguments.getString(KEY_DATE_TO);
        list.stream()
                .filter(s -> list.indexOf(s) % 2 == 0)
                .forEach(s -> cookies.put(s, list.get(list.indexOf(s) + 1)));
        final LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext(),
                LinearLayoutManager.VERTICAL, false);
        binding.recyclerView.setLayoutManager(layoutManager);
        final Drawable dividerDrawable;
        {
            final int[] attrs = {android.R.attr.listDivider};
            @SuppressWarnings("resource")
            final TypedArray typedArray = binding.recyclerView.getContext().obtainStyledAttributes(attrs);
            try {
                dividerDrawable = Objects.requireNonNull(typedArray.getDrawable(0));
            } finally {
                typedArray.recycle();
            }
        }
        binding.recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            private final Rect bounds = new Rect();

            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                TransactionsAdapter adapter = (TransactionsAdapter) parent.getAdapter();
                int position = parent.getChildAdapterPosition(view);
                if (position >= 0 && adapter != null && adapter.isDividerNeeded(position)) {
                    outRect.set(0, 0, 0, dividerDrawable.getIntrinsicHeight());
                } else {
                    outRect.set(0, 0, 0, 0);
                }
            }

            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                TransactionsAdapter adapter = (TransactionsAdapter) parent.getAdapter();
                for (int i = 0, childCount = parent.getChildCount(); i < childCount; i++) {
                    View child = parent.getChildAt(i);
                    int position = parent.getChildAdapterPosition(child);
                    if (position >= 0 && adapter.isDividerNeeded(position)) {
                        parent.getDecoratedBoundsWithMargins(child, bounds);
                        dividerDrawable.setBounds(0, bounds.bottom - dividerDrawable.getIntrinsicHeight(),
                                parent.getWidth(), bounds.bottom);
                        dividerDrawable.draw(c);
                    }
                }
            }
        });
        final StatementViewModel statementViewModel = new ViewModelProvider(this).get(StatementViewModel.class);
        statementViewModel.setCookies(cookies);
        for (Transaction.TransactionType type : Transaction.TransactionType.values()) {
            transactionsAmounts.put(type.ordinal(), 0);
        }

        statementViewModel.getTransactions(dateFrom, dateTo).observe(this, transactions -> {
            binding.recyclerView.setAdapter(new TransactionsAdapter(transactions));
            binding.progressBar.setVisibility(View.GONE);
            binding.title.setVisibility(View.VISIBLE);
            double[] slicePercents = new double[Transaction.TransactionType.values().length];
            transactions.forEach(transaction ->
                    transactionsAmounts.put(transaction.getType(),
                            Objects.requireNonNull(transactionsAmounts.get(transaction.getType())) ==
                            0 ? 1 : transactionsAmounts.get(transaction.getType()) + 1));
            transactionsAmounts.forEach((key, value) -> slicePercents[key] = (double) value / transactions.size() * 100);
            TransactionChart chart = new TransactionChart(slicePercents);
            binding.chartContainer.setImageDrawable(chart);
            binding.colorList.setAdapter(new ColorAdapter(transactionsAmounts));
        });

        statementViewModel.getError().observe(this, error -> {
            String errorMessage;
            switch (error) {
                case ACCOUNT_NUMBER_ERROR:
                    errorMessage = getString(R.string.account_no_error);
                    break;
                case DOWNLOAD_ERROR:
                default:
                    errorMessage = getString(R.string.download_error);
                    break;
            }
            getCallback().returnLogin(errorMessage);
        });
    }

    private enum TransactionStrings {
        ONLINE_PURCHASE(R.string.online_purchase),
        OFFLINE_PURCHASE(R.string.offline_purchase),
        DEPOSIT(R.string.deposit),
        WITHDRAWAL(R.string.withdrawal),
        TRANSFER(R.string.transfer),
        UNKNOWN_TRANSACTION(R.string.unknown_transaction);

        private final int stringId;

        public int getStringId() {
            return stringId;
        }

        TransactionStrings(int stringId) {
            this.stringId = stringId;
        }
    }

    private class ColorAdapter extends BaseAdapter {

        private final ArrayList<Integer> transactionTypes = new ArrayList<>();

        public ColorAdapter(HashMap<Integer, Integer> dataMap) {
            dataMap.forEach((k, v) -> {
                if (v != 0) transactionTypes.add(k);
            });
        }

        @Override
        public int getCount() {
            return transactionTypes.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.color_list_item, parent, false);
            }
            convertView.findViewById(R.id.color_container)
                    .setBackgroundColor(TransactionChart.Colors.values()[transactionTypes.get(position)].getColor());
            ((TextView) convertView.findViewById(R.id.transaction_text))
                    .setText(getString(TransactionStrings.values()[transactionTypes.get(position)].getStringId()));
            return convertView;
        }
    }

    private static class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.ViewHolder> {

        private final ArrayList<Transaction> localDataSet = new ArrayList<>();
        private final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_NEW);

        @SuppressWarnings("Java8ListSort")
        public TransactionsAdapter(ArrayList<Transaction> dataSet) {
            localDataSet.addAll(dataSet);
            Collections.sort(localDataSet, (t1, t2) -> Long.compare(t2.getDate(), t1.getDate()));
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(parent);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction transaction = localDataSet.get(position);
            if (position == 0 || isDividerNeeded(position - 1)) {
                holder.binding.dateContainer.setVisibility(View.VISIBLE);
                holder.binding.dateContainer.setText(dateFormat.format(transaction.getDate()));
            } else {
                holder.binding.dateContainer.setVisibility(View.GONE);
            }
            holder.binding.transactionInfo.setText(transaction.toString());
            holder.binding.balance.setText(transaction.getRemainingBalance());
        }

        @Override
        public int getItemCount() {
            return localDataSet.size();
        }

        public boolean isDividerNeeded(int position) {
            return position < localDataSet.size() - 1 &&
                    localDataSet.get(position).getDate() != localDataSet.get(position + 1).getDate();
        }

        private static class ViewHolder extends RecyclerView.ViewHolder {

            public final TransactionRowItemBinding binding;

            public ViewHolder(ViewGroup parent) {
                this(TransactionRowItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }

            private ViewHolder(TransactionRowItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}
