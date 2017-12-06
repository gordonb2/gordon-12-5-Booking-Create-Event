package booking.sp.clbooking;

/**
 * Created by Ty on 11/5/2017.
 */

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static booking.sp.clbooking.ViewActivity.currentItem;

public class EditActivity extends AppCompatActivity {
    private TimePicker timeStart;
    private DatePicker dateStart;
    private TimePicker timeEnd;
    private DatePicker dateEnd;
    private EditText emailText;
    GoogleAccountCredential mCredential = MainActivity.mCredential;
    Calendar calendarStart;
    Calendar calendarEnd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);

        timeStart = findViewById(R.id.timeStart);
        dateStart = findViewById(R.id.dateStart);
        timeEnd = findViewById(R.id.timeEnd);
        dateEnd = findViewById(R.id.dateEnd);
        emailText = findViewById(R.id.emailText);

        //get event and set the ui to the event values
        new EditActivity.GetEntryTask(mCredential).execute();

        final Button saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                calendarStart = new GregorianCalendar(
                        dateStart.getYear(),
                        dateStart.getMonth(),
                        dateStart.getDayOfMonth(),
                        timeStart.getCurrentHour(),
                        timeStart.getCurrentMinute(),
                        00);
                calendarEnd = new GregorianCalendar(
                        dateEnd.getYear(),
                        dateEnd.getMonth(),
                        dateEnd.getDayOfMonth(),
                        timeEnd.getCurrentHour(),
                        timeEnd.getCurrentMinute(),
                        00);
                new EditActivity.EditEntryTask(mCredential).execute();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    private class EditEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        EditEntryTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        //right now only changes time
        public Event editEntryTest() throws IOException {

            // Retrieve the event from the API
            Event event = mService.events().get("primary", currentItem.getId()).execute();

            // Make changes
            DateTime startDateTime = new DateTime(calendarStart.getTime());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            event.setStart(start);

            //event.setSummary("Appointment at Somewhere");

            DateTime endDateTime = new DateTime(calendarEnd.getTime());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);

            // Update the event
            Event updatedEvent = mService.events().update("primary", event.getId(), event).execute();
            return updatedEvent;
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Event doInBackground(Void... params) {
            try {
                return editEntryTest();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Event output) {
            if (output == null) {
                //text.setText("Event is null");
            } else {
                //text.setText(""+output.toString());
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    //text.setText("The following error occurred:\n"
                    // + mLastError.getMessage());
                }
            } else {
                //text.setText("Request cancelled.");
            }
        }
    }

    private class GetEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        GetEntryTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }
        public Event getEntryTest() throws IOException {

            // Retrieve the event from the API
            Event event = mService.events().get("primary", currentItem.getId()).execute();
            return event;
        }

        /**
         * Background task to call Google Calendar API.
         *
         * @param params no parameters needed for this task.
         */
        @Override
        protected Event doInBackground(Void... params) {
            try {
                return getEntryTest();
            } catch (Exception e) {
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onPostExecute(Event output) {

            //set ui to event values
            EventDateTime start = output.getStart();
            final Calendar calS = Calendar.getInstance();
            calS.setTimeInMillis(start.getDateTime().getValue());
            final int minuteS = calS.get(Calendar.MINUTE);
            final int hourS = calS.get(Calendar.HOUR_OF_DAY);
            timeStart.setCurrentHour(hourS);
            timeStart.setCurrentMinute(minuteS);
            dateStart.updateDate(calS.get(Calendar.YEAR), calS.get(Calendar.MONTH), calS.get(Calendar.DAY_OF_MONTH));


            EventDateTime end = output.getEnd();
            final Calendar calE = Calendar.getInstance();
            calE.setTimeInMillis(end.getDateTime().getValue());
            final int minuteE = calE.get(Calendar.MINUTE);
            final int hourE = calE.get(Calendar.HOUR_OF_DAY);
            timeEnd.setCurrentHour(hourE);
            timeEnd.setCurrentMinute(minuteE);
            dateEnd.updateDate(calE.get(Calendar.YEAR), calE.get(Calendar.MONTH), calE.get(Calendar.DAY_OF_MONTH));

            if (output == null) {
                //text.setText("Event is null");
            } else {
                //text.setText(""+output.toString());
            }
        }

        @Override
        protected void onCancelled() {
            if (mLastError != null) {
                if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainActivity.REQUEST_AUTHORIZATION);
                } else {
                    //text.setText("The following error occurred:\n"
                    // + mLastError.getMessage());
                }
            } else {
                //text.setText("Request cancelled.");
            }
        }
    }
}
