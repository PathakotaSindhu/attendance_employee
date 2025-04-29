package com.example.employeeAtt.models;

import java.util.Date;

public class WeeklyAttendanceDTO {

    private Date date;
    private long present;
    private long absent;

    public WeeklyAttendanceDTO() {}

    public WeeklyAttendanceDTO(Date date, long present, long absent) {
        this.date = date;
        this.present = present;
        this.absent = absent;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public long getPresent() {
        return present;
    }

    public void setPresent(long present) {
        this.present = present;
    }

    public long getAbsent() {
        return absent;
    }

    public void setAbsent(long absent) {
        this.absent = absent;
    }
}
