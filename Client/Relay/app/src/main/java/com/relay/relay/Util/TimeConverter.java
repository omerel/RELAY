package com.relay.relay.Util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by omer on 06/06/2017.
 */

public class TimeConverter {

   public static final String FORMATTER_DATE = "yyyy-MM-dd'T'HH:mm:ss.SSSS'Z'";

    public static String convertCalendarToFormattedDateString(Calendar cal){

        DateFormat date = new SimpleDateFormat(FORMATTER_DATE);
        return (date.format(cal.getTime()));
    }
    public static Calendar convertDateStringToFormattedCalendar(String stringDate){
        DateFormat format = new SimpleDateFormat(FORMATTER_DATE);
        Date date = null;
        try {
            date = format.parse(stringDate);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }
}
