package booking.sp.clbooking;

/**
 * Created by Ty on 11/5/2017.
 */

import android.accounts.Account;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.clover.sdk.util.CloverAccount;
import com.clover.sdk.v1.BindingException;
import com.clover.sdk.v1.ClientException;
import com.clover.sdk.v1.ServiceException;
import com.clover.sdk.v3.employees.EmployeeConnector;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

public class CreateActivity extends AppCompatActivity {

    private TimePicker timeStart;
    private DatePicker dateStart;
    private TimePicker timeEnd;
    private DatePicker dateEnd;
    private TextView nameLabel;
    private EditText nameText;
    private TextView emailLabel;
    private EditText emailText;
    private TextView reasonLabel;
    private EditText reasonText;

    GoogleAccountCredential mCredential = MainActivity.mCredential;
    Calendar calendarStart;
    Calendar calendarEnd;
    String nameString;
    String emailString;
    String reasonString = "";

    //Clover variables
    private Account mAccount;
    private EmployeeConnector mEmployeeConnector;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create);

        timeStart = findViewById(R.id.timeStart);
        dateStart = findViewById(R.id.dateStart);
        timeEnd = findViewById(R.id.timeEnd);
        dateEnd = findViewById(R.id.dateEnd);
        nameLabel = findViewById(R.id.nameLabel);
        nameText = findViewById(R.id.nameText);
        emailLabel = findViewById(R.id.emailLabel);
        emailText = findViewById(R.id.emailText);
        reasonLabel = findViewById(R.id.reasonLabel);
        reasonText = findViewById(R.id.reasonText);

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
                nameString = nameText.getText().toString();
                emailString = emailText.getText().toString();
                reasonString += reasonText.getText().toString();
                new CreateEntryTask(mCredential).execute();
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Retrieve Clover account
        if (mAccount == null) {
            mAccount = CloverAccount.getAccount(this);

            if (mAccount == null) {
                return;
            }
        }

        connectEmployee();

        new EmployeeAsyncTask().execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectEmployee();
    }


    private void connectEmployee() {
        disconnectEmployee();

        if (mAccount != null) {
            mEmployeeConnector = new EmployeeConnector(this, mAccount, null);
            mEmployeeConnector.connect();
        }
    }

    private void disconnectEmployee() {
        if (mEmployeeConnector != null) {
            mEmployeeConnector.disconnect();
            mEmployeeConnector = null;
        }
    }

    private class EmployeeAsyncTask extends AsyncTask<Object, Object, com.clover.sdk.v3.employees.Employee> {

        @Override
        protected com.clover.sdk.v3.employees.Employee doInBackground(Object... voids) {
            try {
                return mEmployeeConnector.getEmployees().get(0);
            } catch (RemoteException | ClientException | ServiceException | BindingException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected final void onPostExecute(com.clover.sdk.v3.employees.Employee employee) {
            super.onPostExecute(employee);
            if (employee != null) {
                reasonString += employee.getName() + ": ";

            }
        }

    }

    private class CreateEntryTask extends AsyncTask<Void, Void, Event> {
        private com.google.api.services.calendar.Calendar mService = null;
        private Exception mLastError = null;

        CreateEntryTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.calendar.Calendar.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar API Android Quickstart")
                    .build();
        }

        public Event createEntryTest() throws IOException {
            Event event = new Event()
                    .setSummary(nameString)
                    .setLocation("Rowan University")
                    .setDescription(reasonString);


            DateTime startDateTime = new DateTime(calendarStart.getTime());
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime);
            //.setTimeZone(TimeZone.getDefault().toString());
            //text.setText(""+(TimeZone.getDefault()));
            event.setStart(start);

            DateTime endDateTime = new DateTime(calendarEnd.getTime());
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime);
            event.setEnd(end);

            EventAttendee[] attendees = new EventAttendee[] {
                    new EventAttendee().setEmail(emailString)
            };
            event.setAttendees(Arrays.asList(attendees));

            EventReminder[] reminderOverrides = new EventReminder[] {
                    new EventReminder().setMethod("email").setMinutes(24 * 60) //email reminder 1 day before
            };
            Event.Reminders reminders = new Event.Reminders()
                    .setUseDefault(false)
                    .setOverrides(Arrays.asList(reminderOverrides));
            event.setReminders(reminders);

            String calendarId = "primary";
            event = mService.events().insert(calendarId, event).setSendNotifications(Boolean.TRUE).execute();
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
                return createEntryTest();
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
}
