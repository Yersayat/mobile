package com.example.shivam.stackoverflow;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class AnswerActivity extends ActionBarActivity {

    private final String initialURL = "http://api.stackexchange.com/2.2/";
    private String site = "site=stackoverflow";
    String requestUrl=null;
    ListView answerList;
    JSONArray mJSONArr;
    AnswersAdapter adapter;
    JSONObject ob3,ob2,answersJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_answer);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle extras = getIntent().getExtras();
        String question = extras.getString("QUESTION");
        try {
            question = URLEncoder.encode(question, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        answerList = (ListView)findViewById(R.id.answerList);
        //appending the questionID to the URL
        requestUrl = this.initialURL + "questions/" + question + "/answers?order=desc&sort=activity&filter=withbody&" + this.site;
        Resources r=getResources();
        Drawable d=r.getDrawable(R.color.primary);
        getSupportActionBar().setBackgroundDrawable(d);
        if(isNetworkAvailable()) {
            new JSONTask().execute();
        }
        else
        {
            Toast.makeText(AnswerActivity.this,"Internet Connection is needed to view Answers!",Toast.LENGTH_SHORT).show();
        }
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }


    private JSONObject makeRequest(String url) throws IOException, JSONException {

        JSONObject response;
        String jsonString;

        HttpClient httpclient = new DefaultHttpClient();

        HttpUriRequest request = new HttpGet(url);
        request.addHeader("Accept-Encoding", "gzip");

        HttpResponse resp = httpclient.execute(request);
        StatusLine statusLine = resp.getStatusLine();

        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            Header contentEncoding = resp.getFirstHeader("Content-Encoding");
            InputStream instream = resp.getEntity().getContent();
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                instream = new GZIPInputStream(instream);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
            StringBuilder responseString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseString.append(line);
            }
            jsonString = responseString.toString();
            response = new JSONObject(jsonString);
        } else {
            resp.getEntity().getContent().close();
            throw new IOException(statusLine.getReasonPhrase());
        }

        return response;
    }

    public class JSONTask extends AsyncTask<String,String,JSONObject>
    {
        private ProgressDialog pDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(AnswerActivity.this);
            pDialog.setMessage("Getting Answers ...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(true);
            pDialog.show();
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                answersJson = makeRequest(requestUrl);
            } catch (JSONException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return answersJson;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            if (jsonObject != null) {
                try {
                    mJSONArr = jsonObject.getJSONArray("items");
                    if (mJSONArr.length() == 0) {
                        pDialog.dismiss();
                        Toast.makeText(AnswerActivity.this, "This question does not have any answers yet !", Toast.LENGTH_SHORT).show();
                    } else {
                        Answer answers[] = new Answer[mJSONArr.length()];
                        for (int i = 0; i < mJSONArr.length(); i++) {
                            ob2 = mJSONArr.getJSONObject(i);
                            if (ob2 != null) {
                                ob3 = ob2.getJSONObject("owner");
                                answers[i] = new Answer(ob2.getString("body"), ob3.getString("display_name"), ob2.getString("score"));
                            }
                        }
                        adapter = new AnswersAdapter(AnswerActivity.this,
                                R.layout.answer_list_item, answers);
                        answerList.setAdapter(adapter);
                        pDialog.dismiss();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_answer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        //For home UP navigation
        if(id==R.id.home)
        {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
