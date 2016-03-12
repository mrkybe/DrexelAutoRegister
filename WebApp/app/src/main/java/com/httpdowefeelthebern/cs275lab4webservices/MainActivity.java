package com.httpdowefeelthebern.cs275lab4webservices;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    TextView textViewResponse;
    EditText urlText;
    EditText userIDText;
    EditText userPasswordText;
    EditText CRNsText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textViewResponse = (TextView)findViewById(R.id.textViewResponse);
        urlText = (EditText)findViewById(R.id.editTextURL);
        userIDText = (EditText)findViewById(R.id.editTextUserID);
        userPasswordText = (EditText)findViewById(R.id.editTextPassword);
        CRNsText = (EditText)findViewById(R.id.editTextCRNs);
    }

    public void click_send(View view)
    {
        try{
            sendData();
        }catch(Exception e){print(e.getLocalizedMessage());}
    }

    private void sendData() throws Exception
    {
        MakeRequestTask SendDataTask = new MakeRequestTask();
        String url = urlText.getText().toString();
        String email = userIDText.getText().toString();
        String pass = userPasswordText.getText().toString();
        String crns = CRNsText.getText().toString();

        Data d = new Data(url, email, pass, crns);
        SendDataTask.execute(d);
    }

    class MakeRequestTask extends AsyncTask<Data, String, String> {
        public MakeRequestTask()
        {
        }

        @Override
        protected String doInBackground(Data... datas) {
            String responseString = "";
            try {
                for(Data data : datas)
                {
                    print("Trying URL: " + data.url + "/register_user");
                    data.url = data.url + "/register_user";
                    URL url = new URL(data.url);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.addRequestProperty("id", data.email);
                    urlConnection.addRequestProperty("email",data.email);
                    urlConnection.addRequestProperty("password",data.pass);
                    responseString = urlConnection.toString() + "\n\n";
                    //urlConnection.addRequestProperty("crns",data.crns);
                    //responseString = urlConnection.getErrorStream()
                    print("test");
                    String line;
                    BufferedReader reader = new BufferedReader(new
                            InputStreamReader(urlConnection.getErrorStream()));
                    while ((line = reader.readLine()) != null) {
                        responseString += line;
                        System.out.println(line);
                    }
                    reader.close();
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            textViewResponse.setText(result);
        }
    }

    class Data
    {
        public String url;
        public String email;
        public String pass;
        public String crns;

        public Data(String url, String email, String pass, String crns) {
            this.url = url;
            this.email = email;
            this.pass = pass;
            this.crns = crns;
        }
    }

    private void print(String str)
    {
        System.out.println(str);
    }
}
