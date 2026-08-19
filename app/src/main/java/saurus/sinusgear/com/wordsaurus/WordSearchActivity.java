package saurus.sinusgear.com.wordsaurus;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import saurus.sinusgear.com.wordsaurus.databinding.ActivityWordSearchBinding;

public class WordSearchActivity extends AppCompatActivity {

    private static final String TAG = "WordSearchActivity";

    private ActivityWordSearchBinding binding;
    private WordAdapter resultAdapter;
    private SQLiteDatabase database;
    private List<Map<String, String>> resultList;
    private ActivityResultLauncher<Intent> pickDbLauncher;

    private HashMap<String, String> createEntry(String name, String description, int rank) {
        HashMap<String, String> item = new HashMap<>();
        item.put("key", name);
        item.put("descr", description);
        if (rank == 1) {
            item.put("rank", "#ffffff");
        } else if (rank == 2) {
            item.put("rank", "#eeeeee");
        } else if (rank == 3) {
            item.put("rank", "#dddddd");
        }

        return item;
    }

    private String normalizeQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "";
        }
        return query.replace("+", "%").replace(".", "_").replace("?", "%");
    }

    private String[] getSearchTokens(String query) {
        if (query.isEmpty()) {
            return new String[0];
        }
        return query.split(" ");
    }

    private void addEmptyRecord() {
        resultList.add(createEntry("Žádný výsledek...", "", 0));
    }

    private void executeFastSearch(String key, String description) {
        if (database == null) {
            return;
        }

        String searchKey = normalizeQuery(key);
        String[] searchTokens = getSearchTokens(normalizeQuery(description));

        List<String> searchArguments = new ArrayList<>();

        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT docid,\n" +
                "       record_key,\n" +
                "       record_descr,\n" +
                "       MIN(rank) AS rank\n" +
                "FROM (\n");

        if ((!searchKey.isEmpty()) && (searchTokens.length == 0)) {
            queryString.append("      SELECT docid,\n" +
                    "             record_key,\n" +
                    "             record_descr,\n" +
                    "             1 AS rank\n" +
                    "      FROM ftsdict\n" +
                    "      WHERE record_key LIKE ?\n");
            searchArguments.add(searchKey);
        }

        String searchToken = normalizeQuery(description);
        if (!searchToken.isEmpty()) {
            queryString.append("      SELECT docid,\n" +
                    "             record_key,\n" +
                    "             snippet(ftsdict) as record_descr,\n" +
                    "             2 AS rank\n" +
                    "      FROM ftsdict\n" +
                    "      WHERE record_descr ");
            queryString.append(" MATCH ? ");
            searchArguments.add(searchToken);

            if (!searchKey.isEmpty()) {
                queryString.append(" AND record_key LIKE ? ");
                searchArguments.add(searchKey);
            }

            queryString.append("      UNION ALL\n" +
                    "      SELECT docid,\n" +
                    "             record_key,\n" +
                    "             snippet(ftsdict) as record_descr,\n" +
                    "             3 AS rank\n" +
                    "      FROM ftsdict\n" +
                    "      WHERE record_descr ");
            queryString.append(" MATCH ? ");
            searchToken = searchToken + "*";
            searchToken = searchToken.replace(" ", "* ");
            searchArguments.add(searchToken);

            if (!searchKey.isEmpty()) {
                queryString.append(" AND record_key LIKE ? ");
                searchArguments.add(searchKey);
            }
        }

        queryString.append(") GROUP BY docid\n" +
                "ORDER BY " +
                " MIN(rank), record_key COLLATE NOCASE");

        queryString.append(" LIMIT 500");

        Cursor cursor = database.rawQuery(queryString.toString(), searchArguments.toArray(new String[0]));

        resultList.clear();

        String resultKey;
        String resultValue;
        int resultRank;

        String resultCountString = "0";

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                resultKey = cursor.getString(cursor.getColumnIndex("record_key"));
                resultValue = cursor.getString(cursor.getColumnIndex("record_descr"));
                resultRank = cursor.getInt(cursor.getColumnIndex("rank"));
                resultList.add(createEntry(resultKey, resultValue, resultRank));
            } while (cursor.moveToNext());
            cursor.close();
            if (resultList.size() < 499) {
                resultCountString = String.valueOf(resultList.size());
            } else {
                resultCountString = "500+";
            }
        } else {
            addEmptyRecord();
        }

        binding.resultCountTextView.setText(resultCountString);
        resultAdapter.notifyDataSetInvalidated();
    }

    private String[] getSlowSearchTokens(String query) {
        String[] tokens = query.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].length() == 0) {
                continue;
            }

            if (!tokens[i].contains("%")) {
                tokens[i] = "%" + tokens[i] + "%";
            }
        }

        return tokens;
    }

    private String normalizeSlowQuery(String query) {
        if (query == null || query.isEmpty()) {
            return "%";
        }
        return query.replace("+", "%").replace(".", "_").replace("?", "%");
    }

    private void executeSlowSearch(String key, String description) {
        if (database == null) {
            return;
        }

        String searchKey = normalizeSlowQuery(key);
        String[] searchTokens = getSlowSearchTokens(normalizeSlowQuery(description));

        List<String> searchArguments = new ArrayList<>();
        searchArguments.add(searchKey);

        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT record_key, record_descr FROM dictionary WHERE record_key LIKE ? ");

        for (String searchToken : searchTokens) {
            queryString.append(" and record_descr LIKE ? ");
            searchArguments.add(searchToken);
        }
        queryString.append(" LIMIT 100");
        Cursor cursor = database.rawQuery(queryString.toString(), searchArguments.toArray(new String[0]));

        resultList.clear();

        String resultKey;
        String resultValue;

        String resultCountString = "0";

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                resultKey = cursor.getString(cursor.getColumnIndex("record_key"));
                resultValue = cursor.getString(cursor.getColumnIndex("record_descr"));
                resultList.add(createEntry(resultKey, resultValue, 0));
            } while (cursor.moveToNext());
            cursor.close();
            if (resultList.size() < 99) {
                resultCountString = String.valueOf(resultList.size());
            } else {
                resultCountString = "100+";
            }
        } else {
            addEmptyRecord();
        }

        binding.resultCountTextView.setText(resultCountString);
        resultAdapter.notifyDataSetInvalidated();
    }

    private void onSearch(View view, EditText wordText, EditText descriptionText) {
        String searchKey = "";
        String searchDescription = "";

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        if (wordText != null) {
            searchKey = wordText.getText().toString();
        }

        if (descriptionText != null) {
            searchDescription = descriptionText.getText().toString();
        }

        if ((searchKey.isEmpty()) && (searchDescription.isEmpty())) {
            return;
        }

        if (binding.slowSearchSwitch.isChecked()) {
            executeSlowSearch(searchKey, searchDescription);
        } else {
            executeFastSearch(searchKey, searchDescription);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWordSearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        final EditText wordText = binding.wordText;
        final EditText descriptionText = binding.descriptionText;

        View.OnKeyListener onKeyListener = new View.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            onSearch(view, wordText, descriptionText);
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        };

        wordText.setOnKeyListener(onKeyListener);
        descriptionText.setOnKeyListener(onKeyListener);

        resultList = new ArrayList<>();

        binding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSearch(view, wordText, descriptionText);
            }
        });

        binding.newSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wordText.setText("");
                descriptionText.setText("");
            }
        });

        pickDbLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        String path = copyFromUri(uri);
                        if (path != null) {
                            database = openInternalDb(path);
                        } else {
                            Toast.makeText(this, "Failed to load dictionary", Toast.LENGTH_LONG).show();
                        }
                    }
                });

        initDb();

        addEmptyRecord();

        resultAdapter = new WordAdapter(this, resultList, R.layout.list_item_2_one_line, new String[]{"key", "descr"}, new int[]{R.id.wordKey, R.id.wordDescription});
        binding.resultListView.setSelectionFromTop(0, 0);
        binding.resultListView.setAdapter(resultAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    private void initDb() {
        File destDir = new File(getFilesDir(), "db");
        destDir.mkdirs();
        File destFile = new File(destDir, "vks.db");

        if (destFile.exists()) {
            Log.i(TAG, "Using existing DB: " + destFile.getAbsolutePath() + " (" + (destFile.length() / 1024) + " KB)");
            database = openInternalDb(destFile.getAbsolutePath());
            return;
        }

        Log.i(TAG, "Dictionary not loaded — launching file picker.");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        String[] mimeTypes = {"application/octet-stream", "database/sqlite"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        try {
            pickDbLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "SAF picker failed: " + e.getMessage());
        }
    }

    private String copyFromUri(Uri uri) {
        File destDir = new File(getFilesDir(), "db");
        destDir.mkdirs();
        File destFile = new File(destDir, "vks.db");
        try (java.io.InputStream in = getContentResolver().openInputStream(uri);
             java.io.OutputStream out = new java.io.FileOutputStream(destFile)) {
            if (in == null) {
                Log.e(TAG, "Cannot open InputStream for URI");
                return null;
            }
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            Log.i(TAG, "DB copied via SAF: " + destFile.getAbsolutePath() + " (" + (destFile.length() / 1024) + " KB)");
            return destFile.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG, "copyFromUri error: " + e.getMessage());
            return null;
        }
    }

    private SQLiteDatabase openInternalDb(String path) {
        try {
            SQLiteDatabase db = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY);
            Log.i(TAG, "DB opened: " + path);
            return db;
        } catch (Exception e) {
            Log.e(TAG, "openDatabase error: " + e.getMessage());
            return null;
        }
    }

}
