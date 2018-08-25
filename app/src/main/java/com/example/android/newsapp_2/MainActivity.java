package com.example.android.newsapp_2;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<List<News>> {

    public static final String LOG_TAG = MainActivity.class.getName();
    //The url to get the data from
    private static final String USGS_BASE_URL =
            "https://content.guardianapis.com/search?";
    private static final String API_KEY = "api-key";
    private static final int NEWS_LOADER_ID = 1;
    public ArrayList<News> newsList;
    public String keyWordSearch;
    private RecyclerViewEmptySupport recyclerView;
    private NewsAdapter newsAdapter;
    private TextView mEmptyTextView;
    private View loadingIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //define the recyclerView which shall later show the News
        recyclerView = findViewById(R.id.list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        //Set an emptyView in case there is no data available.
        mEmptyTextView = findViewById(R.id.empty_view);
        recyclerView.setEmptyView(mEmptyTextView);

        //Display the loading spinner while the data is loaded.
        loadingIndicator = findViewById(R.id.loading_spinner);

        //initialize the currentNews Adapter
        newsList = new ArrayList();
        newsAdapter = new NewsAdapter(newsList, this);
        recyclerView.setAdapter(newsAdapter);
        newsAdapter.setOnItemClickListener(new NewsAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {

                //get the current News Item
                News clickedNews = newsList.get(position);

                // Convert the String URL into a URI object (to pass into the Intent constructor)
                Uri newsUri = Uri.parse(clickedNews.getUrl());

                // Create a new intent to view the news URI
                Intent websiteIntent = new Intent(Intent.ACTION_VIEW, newsUri);

                // Start the intent to launch a new activity
                startActivity(websiteIntent);
            }
        });

        //Check if the device is connected to the internet and only initate the loader when a
        //the device is connected.
        getActiveNetworkInfo();
        if (getActiveNetworkInfo() != null
                && getActiveNetworkInfo().isConnectedOrConnecting()) {
            // Start the AsyncTask to fetch the news data
            Log.i(LOG_TAG, "Loader Initiated");

            getLoaderManager().initLoader(NEWS_LOADER_ID, null, this);
        } else {
            //Load the empty state with the no_connction string if the device is not connected to
            //the interet.
            loadingIndicator.setVisibility(View.GONE);
            mEmptyTextView.setText(R.string.no_connection);
        }

    }

    @Override
    public Loader<List<News>> onCreateLoader(int i, Bundle bundle) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // getString retrieves a String value from the preferences.
        // The second parameter is the default value for this preference.
        keyWordSearch = sharedPrefs.getString(getString(R.string.settings_keyword_key),
                getString(R.string.settings_keyword_default));

        String orderBy = sharedPrefs.getString(getString(R.string.settings_order_by_key),
                getString(R.string.settings_order_by_default));

        // Get the entered keyword and replace potential empty space, numbers and special signs
        // from the entered keyword
        String keyWord = keyWordSearch.replace(" ", "")
                .replaceAll("[^a-zA-Z0-9]", "").toLowerCase();


        // parse breaks apart the URI string that's passed into its parameter
        Uri baseUri = Uri.parse(USGS_BASE_URL);

        // buildUpon prepares the baseUri that we just parsed so we can add query parameters to it
        Uri.Builder uriBuilder = baseUri.buildUpon();

        uriBuilder.appendQueryParameter(getResources().getString(R.string.settings_keyword_key),
                keyWord);
        uriBuilder.appendQueryParameter(getResources().getString(R.string.url_show_fields),
                getResources().getString(R.string.url_byline));
        uriBuilder.appendQueryParameter(getResources().getString(R.string.settings_order_by_key),
                orderBy);
        uriBuilder.appendQueryParameter(API_KEY, getResources().getString(R.string.api_key));

        //return the complete uri with the user settings
        return new NewsLoader(this, uriBuilder.toString());
    }

    @Override
    public void onLoadFinished(Loader<List<News>> loader, List<News> newsList) {
        Log.i(LOG_TAG, "Loader Finished");
        //Clear the adapter of old data
        newsAdapter.clear();
        loadingIndicator.setVisibility(View.GONE);
        String wrongKey = getString(R.string.no_data, keyWordSearch);
        mEmptyTextView.setText(wrongKey);

        if (newsList != null && !newsList.isEmpty()) {
            newsAdapter.addAll(newsList);
        }

    }

    @Override
    public void onLoaderReset(Loader<List<News>> loader) {
        Log.i(LOG_TAG, "Loader Resetted");
        newsAdapter.clear();
    }

    //Check if the device is connected to the internet.
    public NetworkInfo getActiveNetworkInfo() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo;
    }

    @Override
    // This method initialize the contents of the Activity's options menu.
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    // This method is called when an item in the options menu is selected.
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            //Check if SettingsActivity is not empty.
            if (settingsIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(settingsIntent);
                return true;
            }
            Log.i(LOG_TAG, "SettingsActivity not found");
            return false;
        }
        return super.onOptionsItemSelected(item);
    }
}
