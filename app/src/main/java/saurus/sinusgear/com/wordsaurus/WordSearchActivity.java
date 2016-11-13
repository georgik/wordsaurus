package saurus.sinusgear.com.wordsaurus;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordSearchActivity extends AppCompatActivity {

    private HashMap<String, String> createEntry(String name, String description, int rank) {
        HashMap<String, String> item = new HashMap<String, String>();
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

    private ListView resultView;
    private WordAdapter resultAdapter;
    private SQLiteDatabase database;
    private List<Map<String, String>> resultList;

    private String normalizeQuery(String query) {
        String result = "";

        if (query == null) {
            return "";
        }

        if (query.isEmpty()) {
            return "";
        }

        result = query.replace("+", "%").replace(".", "_").replace("?", "%");

        return result;
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

        // Exact match on search key
        String searchKey = normalizeQuery(key);

        // Full text search on description
        String[] searchTokens = getSearchTokens(normalizeQuery(description));

        List<String> searchArguments = new ArrayList<>();


        StringBuilder queryString = new StringBuilder();
        queryString.append("SELECT docid,\n" +
                "       record_key,\n" +
                "       record_descr,\n" +
                "       MIN(rank) AS rank\n" +
                "FROM (\n");

        // Match on: (key, value) - ("abc", null)
        if ((!searchKey.isEmpty()) && (searchTokens.length == 0)) {
            queryString.append("      SELECT docid,\n" +
                    "             record_key,\n" +
                    "             record_descr,\n" +
                    "             1 AS rank\n" +
                    "      FROM ftsdict\n" +
                    "      WHERE record_key LIKE ?\n");
            searchArguments.add(searchKey);
        }

        // Rank 2 - exact match on description
        // Rank 3 - full text match on description
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

        Cursor cursor = database.rawQuery(queryString.toString(), searchArguments.toArray(new String[searchArguments.size()]));

        resultList.clear();


        String resultKey;
        String resultValue;
        int resultRank;

        String resutlCountString = "0";

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
                resutlCountString = String.valueOf(resultList.size());
            } else {
                resutlCountString = "500+";
            }
        } else {
            addEmptyRecord();
        }

        TextView resultCountTextView = (TextView) findViewById(R.id.resultCountTextView);
        resultCountTextView.setText(resutlCountString);

        resultAdapter.notifyDataSetInvalidated();
    }

    private String[] getSlowSearchTokens(String query) {
        String[] tokens = query.split(" ");
        for (int i=0; i<tokens.length; i++) {
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
        String result = "";

        if (query == null) {
            return "%";
        }

        if (query.isEmpty()) {
            return "%";
        }

        result = query.replace("+", "%").replace(".", "_").replace("?", "%");

        return result;
    }

    private void executeSlowSearch(String key, String description) {
        if (database == null) {
            return;
        }
         // Exact match on search key
        String searchKey = normalizeSlowQuery(key);

        // Full text search on description
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
        Cursor cursor = database.rawQuery(queryString.toString(), searchArguments.toArray(new String[searchArguments.size()]));

        resultList.clear();


        String resultKey;
        String resultValue;

        String resutlCountString = "0";

        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                resultKey = cursor.getString(cursor.getColumnIndex("record_key"));
                resultValue = cursor.getString(cursor.getColumnIndex("record_descr"));
                resultList.add(createEntry(resultKey, resultValue, 0));
            } while (cursor.moveToNext());
            cursor.close();
            if (resultList.size() < 99) {
                resutlCountString = String.valueOf(resultList.size());
            } else {
                resutlCountString = "100+";
            }
        } else {
            addEmptyRecord();
        }

        TextView resultCountTextView = (TextView) findViewById(R.id.resultCountTextView);
        resultCountTextView.setText(resutlCountString);

        resultAdapter.notifyDataSetInvalidated();
//        progress.dismiss();
    }

    private void onSearch(View view, EditText wordText, EditText descriptionText) {
        String searchKey = "";
        String searchDescription = "";

        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);

        if (wordText != null) {
            searchKey = wordText.getText().toString();
        }

        if (descriptionText != null) {
            searchDescription = descriptionText.getText().toString();
        }

        Switch slowSearchSwitch = (Switch) findViewById(R.id.slowSearchSwitch);

        if ((searchKey.isEmpty()) && (searchDescription.isEmpty())) {
            return;
        }

        if (slowSearchSwitch.isChecked()) {
            executeSlowSearch(searchKey, searchDescription);
        } else {
            executeFastSearch(searchKey, searchDescription);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_search);

        final EditText wordText = (EditText) findViewById(R.id.wordText);
        final EditText descriptionText = (EditText) findViewById(R.id.descriptionText);

        // Process Enter key on word or description text
        View.OnKeyListener onKeyListener = new View.OnKeyListener()
        {
            public boolean onKey(View view, int keyCode, KeyEvent event)
            {
                if (event.getAction() == KeyEvent.ACTION_DOWN)
                {
                    switch (keyCode)
                    {
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

        resultList = new ArrayList<Map<String, String>>();

        Button searchButton = (Button) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                onSearch(view, wordText, descriptionText);
            }
        });

        Button newSearchButton = (Button) findViewById(R.id.newSearchButton);
        newSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (wordText != null) {
                    wordText.setText("");
                }

                if (descriptionText != null) {
                    descriptionText.setText("");
                }

            }
        });

        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/vks.db";
        File file = new File(filePath);

        if (file.exists()) {
            database = SQLiteDatabase.openDatabase(filePath, null, SQLiteDatabase.OPEN_READONLY);
        } else {
            database = null;
        }

        addEmptyRecord();

        resultView = (ListView) findViewById(R.id.resultListView);
        resultAdapter = new WordAdapter(this, resultList, R.layout.list_item_2_one_line, new String[]{"key","descr"}, new int[]{R.id.wordKey, R.id.wordDescription});
        resultView.setSelectionFromTop(0,0);
        resultView.setAdapter(resultAdapter);
    }
}
