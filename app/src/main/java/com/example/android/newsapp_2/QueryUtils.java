package com.example.android.newsapp_2;

import android.icu.text.TimeZoneFormat;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.icu.text.TimeZoneFormat.*;

/**
 * Helper methods related to requesting and receiving news data from USGS.
 */

public class QueryUtils {

    public static final String LOG_TAG = QueryUtils.class.getSimpleName();

    //Strings to access the different JSON keys. These are functional Strings who should not be
    //localized.
    private static final String JSON_KEY_RESPONSE = "response";
    private static final String JSON_KEY_RESULTS = "results";
    private static final String JSON_KEY_WEBTITLE = "webTitle";
    private static final String JSON_KEY_SECTIONNAME = "sectionName";
    private static final String JSON_KEY_DATE = "webPublicationDate";
    private static final String JSON_KEY_FIELDS = "fields";
    private static final String JSON_KEY_AUTHOR = "byline";
    private static final String JSON_KEY_URL = "webUrl";

    //String for Unknown date, author or thumbnail.
    private static final String NO_INFORMATION_PROVIDED = "";

    //Variables to handle timeouts in the makeHttpRequest.
    private static final int URL_CONNECTION_READ_TIMEOUT = 10000;
    private static final int URL_CONNECTION_CONNECT_TIMEOUT = 15000;

    //String to handle request method in makeHttpRequest.
    private static final String REQUEST_GET = "GET";

    //Integer for the correct response code in makeHttpRequest.
    private static final int HTTP_CORRECT_RESPONSE_CODE = 200;

    //String for the charset for InputStreamReader.
    private static final String CHARSET_INPUT_STREAM = "UTF-8";

    //Integer to check string length and find empty strings.
    private static final int NO_STRING_PROVIDED = 0;


    private QueryUtils() {
    }

    /**
     * Query the USGS to return a list of {@Link News} via JSON parsing.
     */

    public static List<News> fetchNewsData(String requestUrl) {
        Log.i(LOG_TAG, "fetchNewsData started");

        // Create an URL object
        URL url = createUrl(requestUrl);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem making the HTTP request.", e);
        }

        // Extract the relevant fields from the JSON response and create a list of {@Link News}.
        List<News> newsList = extractFeatureFromJson(jsonResponse);

        // Return the list of {@link Earthquake}s
        return newsList;

    }

    private static List<News> extractFeatureFromJson(String newsJSON) {
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        // Create an empty ArrayList that we can start adding earthquakes to
        List<News> newsList = new ArrayList<>();

        // Try to parse the JSON response string. If there's a problem with the way the JSON
        // is formatted, a JSONException exception object will be thrown.
        // Catch the exception so the app doesn't crash, and print the error message to the logs.
        try {

            // Create two JSONObjects from the JSON response string
            JSONObject baseJsonResponse = new JSONObject(newsJSON);
            JSONObject jsonResults = baseJsonResponse.getJSONObject(JSON_KEY_RESPONSE);

            // Extract the JSONArray associated with the key called "results",
            // which represents a list of results (or "News").
            JSONArray newsArray = jsonResults.getJSONArray(JSON_KEY_RESULTS);

            // For each earthquake in the earthquakeArray, create an {@link Earthquake} object
            for (int i = 0; i < newsArray.length(); i++) {

                // Get a single News at position i within the list of News.
                JSONObject currentNews = newsArray.getJSONObject(i);

                // Extract the value for the key called "webTitle"
                String title = currentNews.getString(JSON_KEY_WEBTITLE);
                if (title.length() == NO_STRING_PROVIDED) {
                    title = NO_INFORMATION_PROVIDED;
                }

                // Extract the value for the key called "sectionName"
                String section = currentNews.getString(JSON_KEY_SECTIONNAME);

                if (section.length() == NO_STRING_PROVIDED) {
                    section = NO_INFORMATION_PROVIDED;
                }

                // Extract the value for the key called "webPublicationDate"
                String date;
                String dateSource = currentNews.getString(JSON_KEY_DATE);
                // Check if a value for "webPublicationDate" is provided. If not, set it to
                // a pre defined value.
                if (dateSource.length() == 0) {
                    date = NO_INFORMATION_PROVIDED;
                } else {
                    //format the date so that it is displayed in another format. For more information
                    //on how to format date:
                    // https://stackoverflow.com/questions/37850841/json-date-string-conversion-in-android
                    SimpleDateFormat sourceDate = new SimpleDateFormat
                            ("yyyy-MM-dd'T'hh:mm:ss'Z'", Locale.UK);
                    try {
                        Date unformattedDate = sourceDate.parse(dateSource);
                        SimpleDateFormat formattedDate = new SimpleDateFormat("dd.MM.yyyy " +
                                "\nHH:mm", Locale.UK);
                        date = formattedDate.format(unformattedDate);
                    } catch (ParseException e) {
                        Log.e(LOG_TAG, "Error parsing date");
                        date = NO_INFORMATION_PROVIDED;
                    }
                }

                // Extract the value for the key called "webUrl"
                String url = currentNews.getString(JSON_KEY_URL);

                if (url.length() == NO_STRING_PROVIDED) {
                    url = NO_INFORMATION_PROVIDED;
                }

                String author = NO_INFORMATION_PROVIDED;

                try {
                    // Create a JSONObject for the key called "fields"
                    JSONObject jsonFields = currentNews.getJSONObject(JSON_KEY_FIELDS);

                    // Extract the value for the key called "byline"
                    author = jsonFields.getString(JSON_KEY_AUTHOR);
                    // Check if a value for "byline" is provided. If not, set it to
                    // a pre defined value.
                    if (author.length() == NO_STRING_PROVIDED) {
                        author = NO_INFORMATION_PROVIDED;
                    }

                } catch (Exception e) {
                    Log.v(LOG_TAG, "no fields available");

                }

                // Create a new {@link News} object with the magnitude, location, time,
                // and url from the JSON response.
                News newsArticle = new News(title, section, date, author, url);

                // Add the new {@link Earthquake} to the list of earthquakes.
                newsList.add(newsArticle);
            }

        } catch (JSONException e) {
            // If an error is thrown when executing any of the above statements in the "try" block,
            // catch the exception here, so the app doesn't crash. Print a log message
            // with the message from the exception.
            Log.e("QueryUtils", "Problem parsing the earthquake JSON results", e);
        }

        // Return the list of newsArticles.
        return newsList;
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Problem building the URL ", e);
        }
        return url;
    }

    /**
     * Make an http-request to the given URL and return a String.
     */

    private static String makeHttpRequest(URL url) throws IOException {

        String jsonResponse = "";

        // If the URL is null, then return early with an empty jsonResponse string.

        if (url == null) {
            return jsonResponse;
        }

        // Initiate the HTTP connection.

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;

        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(URL_CONNECTION_READ_TIMEOUT /* milliseconds */);
            urlConnection.setConnectTimeout(URL_CONNECTION_CONNECT_TIMEOUT /* milliseconds */);
            urlConnection.setRequestMethod(REQUEST_GET);
            urlConnection.connect();

            // If the request was successful (response code 200),
            // then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == HTTP_CORRECT_RESPONSE_CODE) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Problem retrieving the earthquake JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // Closing the input stream could throw an IOException, which is why
                // the makeHttpRequest(URL url) method signature specifies than an IOException
                // could be thrown.
                inputStream.close();
            }
        }
        Log.i(LOG_TAG, "makeHttprequest completed");
        return jsonResponse;

    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream,
                    Charset.forName(CHARSET_INPUT_STREAM));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        Log.i(LOG_TAG, "readFromStream completed");
        return output.toString();
    }

}

