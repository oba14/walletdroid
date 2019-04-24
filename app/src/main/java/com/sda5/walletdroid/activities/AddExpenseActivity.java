package com.sda5.walletdroid.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.sda5.walletdroid.R;
import com.sda5.walletdroid.models.Category;
import com.sda5.walletdroid.models.Expense;
import com.sda5.walletdroid.models.Group;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class AddExpenseActivity extends AppCompatActivity {

    private ArrayList<Group> groups = new ArrayList<>();

    private Spinner sprCategory;
    private Spinner sprBuyer;
    private Spinner sprCurrency;
    private DatePickerDialog.OnDateSetListener mDateSetListener;
    private Group selectedGroup;
    private String selectedCurrency;
    private Button btnPickDate;

    // To save on database
    private EditText etTitle;
    private EditText etAmount;
    private Button addExpenseUsers;
    private Spinner addExpenseSpinner;
    private CheckBox checkBoxGroupExpense;
    private LocalDate selectedDate;
    private Long dateMillisec;
    private Category selectedCategory;
    private ArrayList<String> groupMembersIds = new ArrayList<>();
    private ArrayList<String> expenseUsersId = new ArrayList<>();
    private ArrayList<String> expenseUsersName = new ArrayList<>();
    private String buyerId;
    private HashMap<String, Double> balanceOfExpense;
    private HashMap<String, Double> oldBalanceOfGroup;
    private HashMap<String, Double> balanceToUpdate;
    private double usersShare;
    private double buyerShare;
    private double rate;

    // Firestore database stuff
    private FirebaseFirestore database;

    String currentUserId;
    private FirebaseAuth mAuth;
    private String groupId;

    // onRestoreInstanceState
    static String tempTitle;
    static String tempAmount;
    static LocalDate tempDate;
    static int sprCategoryDefaultItem;
    static int sprCurrencyDefaultItem;
    HashMap<String, Double> CurMap;
    static boolean isGroupExpenseChecked;
    private Boolean done = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);
        btnPickDate = findViewById(R.id.btnPickDate);

        addExpenseUsers = findViewById(R.id.btn_add_expense_users);
        addExpenseSpinner = findViewById(R.id.spr_addExpense_users);
        checkBoxGroupExpense = findViewById(R.id.checkBox_group_expense);

        addExpenseUsers.setVisibility(View.GONE);
        addExpenseSpinner.setVisibility(View.GONE);

        checkBoxGroupExpense.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    isGroupExpenseChecked = true;
                    addExpenseUsers.setVisibility(View.VISIBLE);
                    addExpenseSpinner.setVisibility(View.VISIBLE);

                } else {
                    isGroupExpenseChecked = false;
                    addExpenseUsers.setVisibility(View.GONE);
                    addExpenseSpinner.setVisibility(View.GONE);
                }

            }
        });


        FirebaseApp.initializeApp(this);
        database = FirebaseFirestore.getInstance();

        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        groupId = getIntent().getStringExtra("group_id");
        groupMembersIds = getIntent().getStringArrayListExtra("groupMembersIds");
        expenseUsersId = getIntent().getStringArrayListExtra("expenseUsersIds");
        expenseUsersName = getIntent().getStringArrayListExtra("expenseUsersAccounts");

        etTitle = findViewById(R.id.txt_addExpense_expenseTitle);
        etAmount = findViewById(R.id.txt_addExpense_expenseAmount);

        // Create spinner for currencies
        ArrayList<String> currencies = new ArrayList<>();
        currencies.add("SEK");
        currencies.add("EUR");
        currencies.add("USD");
        currencies.add("NOK");
        currencies.add("DKK");

        sprCurrency = findViewById(R.id.sprCurrency);
        ArrayAdapter<String> adapterCurrency = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, currencies);
        adapterCurrency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sprCurrency.setAdapter(adapterCurrency);
        sprCurrency.setSelection(sprCurrencyDefaultItem);
        sprCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCurrency = (String) parent.getItemAtPosition(position);
                sprCurrencyDefaultItem = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        // Create sample category list for now
        // TODO update this when category is decided by team. it should retrieve data from fire store
        ArrayList<Category> catlist = new ArrayList<>();
        catlist.add(new Category("Food", 2000));
        catlist.add(new Category("Clothes", 3000));
        catlist.add(new Category("Transportation", 5000));

        // Create spinner for user to choose the category of expense
        sprCategory = findViewById(R.id.spr_addExpense_category);
        ArrayAdapter adapterCategory = new ArrayAdapter(this, android.R.layout.simple_spinner_item, catlist);
        adapterCategory.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sprCategory.setAdapter(adapterCategory);
        sprCategory.setSelection(sprCategoryDefaultItem);
        sprCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedCategory = (Category) parent.getItemAtPosition(position);
                sprCategoryDefaultItem = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        if (expenseUsersId != null) {
            sprBuyer = findViewById(R.id.spr_addExpense_users);
            ArrayAdapter adapterBuyer = new ArrayAdapter(this, android.R.layout.simple_spinner_item, expenseUsersName);
            adapterBuyer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            adapterBuyer.notifyDataSetChanged();
            sprBuyer.setAdapter(adapterBuyer);
            sprBuyer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String buyerSelected = (String) parent.getItemAtPosition(position);
                    for (int i = 0; i < expenseUsersName.size(); i++) {
                        if (buyerSelected == expenseUsersName.get(i)) {
                            buyerId = expenseUsersId.get(i);
                        }
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }

        // Get the date of expense from user
        mDateSetListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                selectedDate = LocalDate.of(year, month+1, dayOfMonth);
                String s = " " + dayOfMonth + " - " + (month +1)+ " - " + year;
                btnPickDate.setText(s);
            }
        };
    }

    public void pickDate(View view) {
        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog = new DatePickerDialog(this,
                android.R.style.Theme_Holo_Light_Dialog_MinWidth,
                mDateSetListener,
                year, month, day);
        dialog.show();
    }

    public void addExpenseUsers(View view) {
        if (!etTitle.getText().toString().trim().isEmpty() || !etAmount.getText().toString().trim().isEmpty()) {
            tempTitle = etTitle.getText().toString();
            tempAmount = etAmount.getText().toString();
        }
        Intent intent = new Intent(this, ChooseGroupForExpenseActivity.class);
        startActivity(intent);
    }

    public void saveExpense(View v) {
        if (selectedDate == null) {
            selectedDate = LocalDate.now();
        } else {
            System.out.println("check");
        }


        if (etTitle.getText().toString().trim().isEmpty() ||
                etAmount.getText().toString().trim().isEmpty() ||
                selectedCategory == null ||
                groupId == null ||
                selectedDate == null ||
                expenseUsersId.size() == 0 ||
                buyerId == null){
            Toast.makeText(this, "Please enter all fields first", Toast.LENGTH_SHORT).show();
        }else{
            // Fetch data from API for getting rate for different currencies.
            exchangeRatesMap exchangeRatesMap = new exchangeRatesMap();
            CurMap = exchangeRatesMap.getCurrMap();
            rate = 1 / CurMap.get(selectedCurrency);
            String title = etTitle.getText().toString().trim();
            double amount = rate * Double.parseDouble(etAmount.getText().toString());
            String date = selectedDate.toString();
            dateMillisec = selectedDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            // creating hashmap to update balance in group collection
            balanceOfExpense = new HashMap<>();

            for (String memberId : groupMembersIds) {
                balanceOfExpense.put(memberId, 0.0);
            }

            usersShare = -amount / expenseUsersId.size();
            buyerShare = amount + usersShare;

            for (String usersId : expenseUsersId) {
                if (usersId == buyerId) {
                    balanceOfExpense.put(usersId, buyerShare);
                } else {
                    balanceOfExpense.put(usersId, usersShare);
                }
            }

            // Getting existing group balance hashmap from database

            database.collection("Groups").whereEqualTo("id", groupId).limit(1).get()
                    .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                                Group groupToUpdate = queryDocumentSnapshot.toObject(Group.class);
                                oldBalanceOfGroup = groupToUpdate.getBalance();
                                balanceToUpdate = new HashMap<>(oldBalanceOfGroup);
                                balanceOfExpense.forEach((k, v) -> balanceToUpdate.merge(k, v, (a, b) -> a + b));
                                //update the balance
                                database.collection("Groups").document(groupId).update("balance", balanceToUpdate);
                            }
                        }
                    });

            // creating expense object
            Expense expense = new Expense(title, amount, selectedCategory, buyerId, groupId,
                    date, dateMillisec, expenseUsersId, false);

            database.collection("Expenses").document(expense.getId()).set(expense).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Toast.makeText(AddExpenseActivity.this, "Added successfully", Toast.LENGTH_SHORT).show();
                    etAmount.setText("");
                    etTitle.setText("");
                    sprCategory.setSelection(0);
                    sprCurrency.setSelection(0);
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(AddExpenseActivity.this, "Sth is wrong", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (!etTitle.getText().toString().trim().isEmpty() || !etAmount.getText().toString().trim().isEmpty()) {
            outState.putString("Title", etTitle.getText().toString().trim());
            outState.putString("Amount", etAmount.getText().toString().trim());
            outState.putInt("CategoryPosition", sprCategoryDefaultItem);
            outState.putInt("CurrencyPosition", sprCurrencyDefaultItem);


            tempTitle = etTitle.getText().toString().trim();
            tempAmount = etAmount.getText().toString().trim();
            tempDate = selectedDate;
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        tempTitle = savedInstanceState.getString("Title");
        tempAmount = savedInstanceState.getString("Amount");
        sprCategoryDefaultItem = savedInstanceState.getInt("CategoryPosition");
        sprCurrencyDefaultItem = savedInstanceState.getInt("CurrencyPosition");
    }

    @Override
    protected void onResume() {
        super.onResume();
        etTitle.setText(tempTitle);
        etAmount.setText(tempAmount);
        selectedDate = tempDate;
        if (isGroupExpenseChecked) {
            checkBoxGroupExpense.setChecked(true);
        }
    }
}
