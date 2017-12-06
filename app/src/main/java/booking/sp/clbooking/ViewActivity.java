package booking.sp.clbooking;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.SimpleCursorAdapter;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.clover.sdk.util.CloverAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;

import com.google.api.services.calendar.CalendarScopes;
import com.google.api.client.util.DateTime;

import com.google.api.services.calendar.model.*;


/**
 * Created by Ty on 11/5/2017.
 */

public class ViewActivity extends AppCompatActivity {
    private Context mContext;
    private Activity mActivity;
    public List<Event> events;
    SimpleArrayAdapter adapter;
    ListView listview;
    static Event currentItem;

    GoogleAccountCredential mCredential = MainActivity.mCredential;

    SimpleDateFormat sdf = new SimpleDateFormat("MMMM-dd KK:mm a");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view);

        mContext = getApplicationContext();
        mActivity = ViewActivity.this;
        listview = findViewById(R.id.listView);
        events = MainActivity.events;

        if (events != null) {
            adapter = new SimpleArrayAdapter(this, events);
            listview.setAdapter(adapter);
        }

        final Button backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        final Button createButton = findViewById(R.id.createButton);
        createButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent myIntent = new Intent(mContext, CreateActivity.class);
                startActivityForResult(myIntent, 0);
            }
        });
    }

    @Override
    protected void onResume() {
        Log.i("test", "...Resumed.");
        super.onResume();

        //MainActivity.getResultsFromApi();
    }

    public class SimpleArrayAdapter extends ArrayAdapter<Event> {
        private final Context context;
        private final List<Event> values;

        public SimpleArrayAdapter(Context context, List<Event> values) {
            super(context, -1, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final int mPosition = position;
            final View view = convertView;
            final AdapterView<?> mParent = (AdapterView<?>) parent;

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = null;
            if (inflater != null) {
                rowView = inflater.inflate(R.layout.daylayout, parent, false);
            }

            TextView dayText = rowView.findViewById(R.id.dayText);
            TextView timeText = rowView.findViewById(R.id.timeText);
            TextView descriptionText = rowView.findViewById(R.id.descriptionText);
            Button entryButton = rowView.findViewById(R.id.entryButton);
            Button reminderButton = rowView.findViewById(R.id.reminderButton);
            Button deleteButton = rowView.findViewById(R.id.deleteButton);

            Event e = values.get(position);
            DateTime start = e.getStart().getDateTime();
            if (start == null) {
                // All-day events don't have start times, so just use
                // the start date.
                start = e.getStart().getDate();
            }
            Date sd = new Date(start.getValue());
            Date ed = new Date(e.getEnd().getDateTime().getValue());
            //Title of Event.
            dayText.setText(e.getSummary());
            //Date and Time of Event.
            timeText.setText("" + sdf.format(sd) + " - " + sdf.format(ed));
            //Get the description of the event.
            descriptionText.setText(e.getDescription());
            entryButton.setText("Edit Entry");
            entryButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    currentItem = (Event) mParent.getItemAtPosition(mPosition);
                    Intent myIntent = new Intent(mContext, EditActivity.class);
                    startActivityForResult(myIntent, 0);
                }
            });
            reminderButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{"recipient@example.com"});
                    i.putExtra(Intent.EXTRA_SUBJECT, "subject of email");
                    i.putExtra(Intent.EXTRA_TEXT, "body of email");
                    try {
                        startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(ViewActivity.this, "There are no email clients installed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            deleteButton.setOnClickListener(new View.OnClickListener() {

                public void onClick(View v) {
                    final Event item = (Event) mParent.getItemAtPosition(mPosition);
                    final String itemLabel = item.getSummary();
                    new AlertDialog.Builder(ViewActivity.this)
                            .setTitle("Title")
                            .setMessage("Do you really want to delete " + itemLabel +"?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    new ViewActivity.DeleteEntryTask(mCredential, item).execute();
                                    values.remove(item);
                                    adapter.notifyDataSetChanged();
                                    Toast.makeText(ViewActivity.this, itemLabel + " Successfully Deleted", Toast.LENGTH_SHORT).show();
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                }
            });
            return rowView;
        }
    }


    private class DeleteEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Event mEvent;

        DeleteEntryTask(GoogleAccountCredential credential, Event event) {
            mEvent = event;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        } public Event DeleteEntry() throws IOException {

            String calendarId = "primary";
            mService.events().delete(calendarId, mEvent.getId()).execute();
            return mEvent;
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Event doInBackground(Void... params) {
            try {
                return DeleteEntry();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onCancelled() {
        }

    }
}

