package com.example.hackernews;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
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
    private int numberOfNews = 0;
    private final String topNewsUrl = "https://hacker-news.firebaseio.com/v0/topstories.json";
    private final String newsAPIFormat = "https://hacker-news.firebaseio.com/v0/item/%s.json";
    private ArrayList<String> newsTitles;
    private ArrayList<String> newsUrls;
    private ArrayAdapter<String> adapter;

    private class NewsIdTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... urls) {
            return  this.getNewsIds(this.getData(urls[0]));
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

        private String[] getNewsIds(String jsonString) {
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
        newsTitles = new ArrayList<>();
        newsUrls = new ArrayList<>();
        this.prepareNewsListView();
        this.prepareNews(10);

    }

    private void prepareNews(int targetNumberOfNews) {
        NewsIdTask task = new NewsIdTask();
        try {
            String[] newsIds = task.execute(this.topNewsUrl).get();
            ArrayList<JSONObject> news = new ArrayList<>();
            targetNumberOfNews = targetNumberOfNews < newsIds.length ? targetNumberOfNews : newsIds.length;
            for (int i = this.numberOfNews; i < targetNumberOfNews; i++) {
                String id = newsIds[i];
                String api = String.format(this.newsAPIFormat, id);
                NewsContentDownloadTask newsDownloadTask = new NewsContentDownloadTask();
                news.add(newsDownloadTask.execute(api).get());
            }
            this.numberOfNews = targetNumberOfNews;
            this.prepareTitlesUrlsFromNews(news);
        } catch (ExecutionException | InterruptedException | JSONException e) {
            e.printStackTrace();
        }
    }

    private void prepareNewsListView() {
        ListView listView = findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, newsTitles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), WebBrowseActivity.class);
                intent.putExtra("url", newsUrls.get(i));
                startActivity(intent);
            }
        });

        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int i) {

            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                if (i + i1 == i2) {
                    int numberOfNewsToAdd = 10;
                    prepareNews(numberOfNews + numberOfNewsToAdd);
                    Log.i("ListView", "Scrolled to the end");
                }
            }
        });

    }

    private void prepareTitlesUrlsFromNews(ArrayList<JSONObject> news) throws JSONException {
        for (JSONObject obj : news) {
            if (obj.has("title") && obj.has("url")) {
                newsTitles.add(obj.getString("title"));
                newsUrls.add(obj.getString("url"));
            }
        }
        // Improves performance after moving out from the for loop
        adapter.notifyDataSetChanged();
    }


}
