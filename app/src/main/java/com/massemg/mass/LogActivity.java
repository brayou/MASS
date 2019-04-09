package com.massemg.mass;

import android.os.Bundle;
import android.util.Log;
import android.widget.CalendarView;
import android.widget.CalendarView.OnDateChangeListener;

public class LogActivity extends GraphActivity {
    int dayOfYear = -1;
    String dayStr;
    long date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        graphMode = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);
        initGraph();

        final CalendarView cal = (CalendarView) findViewById(R.id.calendar);
        date = cal.getDate();
        cal.setOnDateChangeListener(new OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month,
                                            int dayOfMonth) {
                //if(cal.getDate() != date) {
                    date = cal.getDate();
                    dayOfYear = dayOfYear(month+1, dayOfMonth, year);
                    Log.d("myTag", "Day changed to " +dayOfYear);
                    dayStr = Integer.toString(dayOfYear);
                    int[][] data = readFile(dayStr);
                    if(data[0][0] == -1) {
                        initGraph();
                        return;
                    }
                    updateGraph(data, true , dayStr, false);
                //}
            }
        });
        Log.d("myTag", "Log: Day of year is " +dayOfYear);

    }
    private static final int[] DAYS_BEFORE = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};

    public static int dayOfYear(int month, int dayOfMonth, int year) {
        int leapDays = month > 2 && (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 1 : 0;
        return DAYS_BEFORE[month - 1] + dayOfMonth + leapDays;
    }
}
