package com.example.hackernews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.NonReadableChannelException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    private final String topNewsUrl = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private final String newsAPIFormat = "https://hacker-news.firebaseio.com/v0/item/%s.json";
    private HashMap<String, String> newsMap;

    private class NewsIdTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... urls) {
            return  this.getNewsNums(this.getData(urls[0]));
        }

        private String getData(String link) {
            HttpURLConnection connection = null;
            String result = null;

            try {
                URL url = new URL(link);
                connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();

                StringBuilder builder = new StringBuilder();
                while (data != -1) {
                    char current = (char) data;
                    builder.append(current);
                    data = reader.read();
                }

                result = builder.toString();

            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return  result;
        }

        private String[] getNewsNums(String jsonString) {
            Pattern pattern = Pattern.compile("([0-9]*),");
            Matcher matcher = pattern.matcher(jsonString);
            ArrayList<String> result = new ArrayList<>();
            while (matcher.find()) {
                result.add(matcher.group(1));
            }
            return result.toArray(new String[0]);
        }
    }

    private class NewsContentDownloadTask extends  AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... urls) {
            HttpURLConnection connection = null;
            JSONObject result = null;

            try {
                URL url = new URL(urls[0]);
                connection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(inputStream);
                int data = reader.read();

                StringBuilder builder = new StringBuilder();
                while (data != -1) {
                    char current = (char) data;
                    builder.append(current);
                    data = reader.read();
                }

                result = new JSONObject(builder.toString());

            } catch (Exception e) {
                e.printStackTrace();

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return  result;
        }

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.prepareNews();
    }

    private void prepareNews() {
        NewsIdTask task = new NewsIdTask();
        try {
            String[] newsIds = task.execute(this.topNewsUrl).get();
            ArrayList<JSONObject> news = new ArrayList<>();

            for (String id : newsIds) {
                String api = String.format(this.newsAPIFormat, id);
                NewsContentDownloadTask newsDownloadTask = new NewsContentDownloadTask();
                news.add(newsDownloadTask.execute(api).get());
            }
            prepareNewsList(news);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void prepareNewsList(final ArrayList<JSONObject> news) {
        ListView listView = findViewById(R.id.listView);
        try {
            newsMap = this.getMapFromNews(news);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, newsMap.keySet().toArray(new String[0]));
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent(getApplicationContext(), WebBrowseActivity.class);
                    intent.putExtra("url", newsMap.get(newsMap.keySet().toArray(new String[0])[i]));
                    startActivity(intent);
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, String> getMapFromNews(ArrayList<JSONObject> news) throws JSONException {
        HashMap<String, String> result = new HashMap<>();
        for (JSONObject obj : news) {
            if (obj.has("title") && obj.has("url")) {
                result.put((String) obj.get("title"), (String) obj.get("url"));
            }
        }
        return result;
    }


}
