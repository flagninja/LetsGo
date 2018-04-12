package t.systematic.letsgo;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import t.systematic.letsgo.AccountManagement.LogInActivity;
import t.systematic.letsgo.Database.DatabaseHelper;
import t.systematic.letsgo.Database.OnGetDataListener;
import t.systematic.letsgo.Meeting.Meeting;
import t.systematic.letsgo.MeetingActivities.MeetingManagerActivity;
import t.systematic.letsgo.UserObject.User;

public class MainActivity extends AppCompatActivity implements  OnGetDataListener{
    private User user;
    private String mUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Gets user name from SharedPreferences for auto-login. */
        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.shared_preference), Context.MODE_PRIVATE);
        String defaultValue = "";
        String username = sharedPref.getString("username", defaultValue);

        //TODO remove when committing
        //Intent i = new Intent(MainActivity.this, LogInActivity.class);
        //startActivity(i);
        //TODO reimplement after bug fixing

        //If username is valid get all of user's info, else take user to login activity.
        if(!username.equals(defaultValue)){
            mUsername = username;
            DatabaseHelper.getInstance().getUserInfo(username, this);
        }
        else {
            Intent i = new Intent(MainActivity.this, LogInActivity.class);
            startActivity(i);
        }


    }

    @Override
    public void onSuccess(DataSnapshot dataSnapshot) {
        /* Initialize user. */
        user = new User(mUsername, new ArrayList<String>(), new ArrayList<Meeting>(), dataSnapshot.child("email").toString(),
                dataSnapshot.child("phone").toString());

        /* Add user's friends. */
        for(DataSnapshot friend : dataSnapshot.child("friends").getChildren()){
            user.addFriend(friend.getValue().toString());
        }

        String checkForPlaceHolder  = dataSnapshot.child("meetings").child("0").getValue().toString();
        /* In the event the user doesn't have any meetings. */
        if(checkForPlaceHolder.equals("null")){
            Intent i = new Intent(MainActivity.this, MeetingManagerActivity.class);
            i.putExtra("USER_OBJECT", user);
            startActivity(i);
            return;
        }
        /* Logic below is to pull all of the user's meetings's information. */
        final int numOfMeetings = (int)dataSnapshot.child("meetings").getChildrenCount() - 1;
        int completed = 0;
        for(DataSnapshot meetings : dataSnapshot.child("meetings").getChildren()){
            final int current = completed;
            DatabaseHelper.getInstance().getUserMeetings(meetings.getValue().toString(), new OnGetDataListener() {
                @Override
                public void onSuccess(DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()){
                        String meetingName = dataSnapshot.child("meetingName").getValue().toString();
                        String calendarValues = dataSnapshot.child("startTime").getValue().toString();
                        String admin = dataSnapshot.child("admin").getValue().toString();
                        /* Set date into Calendar object. */
                        Calendar calendar = Calendar.getInstance();
                        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                        try {
                            calendar.setTime(sdf.parse(calendarValues));// all done
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        ArrayList<String> participants = new ArrayList<String>();
                        for(DataSnapshot dbParticipants : dataSnapshot.child("participants").getChildren()){
                            participants.add(dbParticipants.getValue().toString());
                        }
                        /* Adding a new meeting with info pulled from DB. */
                        user.addNewMeeting(new Meeting(meetingName, participants, calendar,
                                Double.parseDouble(dataSnapshot.child("Lat").getValue().toString()),
                                Double.parseDouble(dataSnapshot.child("Long").getValue().toString()),
                                dataSnapshot.getKey().toString(), admin));
                    }else{
                        Toast.makeText(getApplicationContext(), "Failed to pull user meetings.", Toast.LENGTH_LONG).show();
                    }
                    /* Once we are done pulling all info from user go to MeetingManangerActivity. */
                    if(current == numOfMeetings){
                        Intent i = new Intent(MainActivity.this, MeetingManagerActivity.class);
                        i.putExtra("USER_OBJECT", user);
                        startActivity(i);
                    }
                }
                @Override
                public void onFailure(String failure) {

                }
            });//Database
            completed++;
        }//Forloop
    }

    @Override
    public void onFailure(String failure) {
        Toast.makeText(getApplicationContext(), failure, Toast.LENGTH_SHORT).show();
    }

}
