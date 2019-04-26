package com.sda5.walletdroid.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sda5.walletdroid.R;
import com.sda5.walletdroid.adapters.ExpenseAdapter;
import com.sda5.walletdroid.adapters.GroupAdapter;
import com.sda5.walletdroid.models.Account;
import com.sda5.walletdroid.models.Expense;
import com.sda5.walletdroid.models.Group;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Optional;

public class ExpenseFragment extends Fragment {
    private ExpenseAdapter expenseAdapter;
    private ArrayList<Expense> expenses = new ArrayList<>();
    FirebaseFirestore database;
    private FirebaseAuth mAuth;
    private String accountId;
    String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_expense, null);
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        ListView listView = v.findViewById(R.id.expense_list);
        listView.setScrollingCacheEnabled(false);

        expenseAdapter = new ExpenseAdapter(v.getContext(), expenses);
        listView.setAdapter(expenseAdapter);

        database.collection("Accounts").whereEqualTo("userID", currentUserId).get().addOnCompleteListener(
                task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot accountSnapshot = task.getResult();
                        if (null != accountSnapshot) {
                            Optional<Account> account = accountSnapshot.toObjects(Account.class).stream().findFirst();
                            if (account.isPresent()) {
                                accountId = account.get().getId();
                                database.collection("Expenses")
                                        .whereArrayContains("expenseAccountIds", accountId)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                                Expense expense = documentSnapshot.toObject(Expense.class);
                                                expenses.add(expense);
                                            }
                                            expenseAdapter.notifyDataSetChanged();
                                        });
                            }
                        }
                    }
                }
        );
        return v;
    }
}
