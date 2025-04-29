package com.example.employeeAtt.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


import java.time.LocalDate;
import java.util.*;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Service;

import com.example.employeeAtt.models.Attendance;
import com.example.employeeAtt.models.Employee;
import com.example.employeeAtt.models.Institute;
import com.example.employeeAtt.models.WeeklyAttendanceDTO;
import com.example.employeeAtt.repositories.AttendanceRepository;
import com.example.employeeAtt.repositories.EmployeeRepository;
import com.example.employeeAtt.repositories.InstituteRepository;

@Service
public class AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private InstituteRepository instituteRepository;

    // Fetch the authenticated employee from SecurityContext
    private Employee getAuthenticatedEmployee() {
        String username = ((User) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUsername();
        return employeeRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated employee not found"));
    }

    // Mark attendance for the employee
    public Attendance markAttendance(String employeeId, Long instituteId, String remarks) {
        // Step 1: Fetch employee and institute
        Employee employee = employeeRepository.findById(employeeId)
            .orElseThrow(() -> new RuntimeException("Employee not found"));

        Institute institute = instituteRepository.findById(instituteId)
            .orElseThrow(() -> new RuntimeException("Institute not found"));

        // Step 2: Fetch today's attendance records for the employee
        List<Attendance> todayAttendance = attendanceRepository.findTodayAttendanceByEmployee(employeeId);

        // Step 3: Determine attendance type (Login/Logout)
        String attendanceType = "Login";
        if (!todayAttendance.isEmpty()) {
            Attendance lastEntry = todayAttendance.get(todayAttendance.size() - 1);
            if ("Login".equalsIgnoreCase(lastEntry.getAttendanceType())) {
                attendanceType = "Logout";
            }
        }

        // Step 4: Validate remarks for Logout attendance
        if ("Logout".equals(attendanceType)) {
            if (remarks == null || remarks.trim().isEmpty()) {
                throw new RuntimeException("Remarks are required for Logout attendance.");
            }
        } else {
            if (remarks != null && !remarks.trim().isEmpty()) {
                throw new RuntimeException("Remarks should not be provided for Login attendance.");
            }
        }
        
        // Step 5: Create and save new attendance record
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setInstitute(institute);
        attendance.setAttendanceType(attendanceType);
        attendance.setLoginTime(new Date());

        if ("Logout".equals(attendanceType)) {
            attendance.setRemarks(remarks); // Required for Logout
        } else {
            attendance.setRemarks(null); // No remark for Login
        }

        return attendanceRepository.save(attendance);
    }

    // Get all attendance records
    public List<Attendance> getAllAttendance() {
        return attendanceRepository.findAll();
    }

    // Get attendance for a specific date
    public List<Attendance> findAttendanceByDate(LocalDate date) {
        return attendanceRepository.findByLoginDate(date);
    }
    
    // Get absentees for a specific date
    public List<Employee> getAbsenteesForDate(LocalDate date) {
        List<Employee> allEmployees = employeeRepository.findAll();
        List<Attendance> attendanceRecords = attendanceRepository.findByLoginDate(date);

        List<String> attendedEmployeeIds = attendanceRecords.stream()
                .map(a -> a.getEmployee().getEmployeeId())
                .collect(Collectors.toList());

        // Return employees who are not in the attendance list
        return allEmployees.stream()
                .filter(emp -> !attendedEmployeeIds.contains(emp.getEmployeeId()))
                .collect(Collectors.toList());
    }

    // Get absentees for today
    public List<Employee> getTodayAbsentees() {
        return getAbsenteesForDate(LocalDate.now());
    }

    // // Get weekly attendance report (present/absent count per day)
    // public Map<LocalDate, Map<String, Long>> getCurrentWeekReport() {
    //     LocalDate today = LocalDate.now();
    //     LocalDate monday = today.with(DayOfWeek.MONDAY);
    //     LocalDate saturday = monday.plusDays(5);

    //     // Fetch all employee IDs
    //     List<String> allEmployeeIds = employeeRepository.findAll()
    //         .stream()
    //         .map(Employee::getEmployeeId)
    //         .collect(Collectors.toList());

    //     Map<LocalDate, Map<String, Long>> report = new LinkedHashMap<>();

    //     for (LocalDate date = monday; !date.isAfter(saturday); date = date.plusDays(1)) {
    //         List<String> presentIds = attendanceRepository.findPresentEmployeeIdsByDate(date);
    //         long presentCount = presentIds.size();
    //         long absentCount = allEmployeeIds.size() - presentCount;

    //         Map<String, Long> dailyStats = new HashMap<>();
    //         dailyStats.put("present", presentCount);
    //         dailyStats.put("absent", absentCount);

    //         report.put(date, dailyStats);
    //     }

    //     return report;
    // }
    
    // public List<Attendance> getWeeklyReport(Date startDate, Date endDate) {
    //     return attendanceRepository.findByLoginTimeBetween(startDate, endDate);
    // }
    public long getAttendanceCount() {
        return attendanceRepository.count();
    }
    public long getAbsentCount() {
        long totalEmployees = employeeRepository.count();
        long presentEmployees = attendanceRepository.count(); // or your existing logic
        return totalEmployees - presentEmployees;
    }


    //  private final AttendanceRepository attendanceRepository;
    // private final EmployeeRepository employeeRepository;

    // Constructor injection for AttendanceRepository and EmployeeRepository
    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // public List<WeeklyAttendanceDTO> getWeeklyAttendance() {
    //     // Date 7 days ago
    //     Date sevenDaysAgo = java.sql.Date.valueOf(LocalDate.now().minusDays(7));

    //     // Fetch the raw attendance data
    //     List<Object[]> rawData = attendanceRepository.getWeeklyPresentCounts(sevenDaysAgo);

    //     // Get the total number of employees
    //     long totalEmployees = employeeRepository.count();

    //     // Map the raw data to WeeklyAttendanceDTO
    //     return rawData.stream()
    //         .map(row -> {
    //             Date date = (Date) row[0];
    //             long present = ((Number) row[1]).longValue();
                
    //             // Calculate absent employees
    //             long absent = totalEmployees - present;

    //             return new WeeklyAttendanceDTO(date, present, absent);
    //         })
    //         .collect(Collectors.toList());
    // }

    public List<Map<String, Object>> getWeeklyAttendance() {
        LocalDate today = LocalDate.now();

        // Get Monday of the current week
        LocalDate startOfWeek = today.with(DayOfWeek.MONDAY);
        LocalDate endOfWeek = startOfWeek.plusDays(5); // Monday to Saturday

        List<Object[]> rawData = attendanceRepository.getWeeklyPresentCounts(java.sql.Date.valueOf(startOfWeek));

        Map<LocalDate, Long> presentMap = new HashMap<>();
        for (Object[] row : rawData) {
            java.sql.Date sqlDate = (java.sql.Date) row[0];
            LocalDate localDate = sqlDate.toLocalDate();
            long present = ((Number) row[1]).longValue();
            presentMap.put(localDate, present);
        }

        long totalEmployees = employeeRepository.count();

        List<Map<String, Object>> finalReport = new ArrayList<>();

        // Loop from Monday to Saturday
        for (LocalDate date = startOfWeek; !date.isAfter(endOfWeek); date = date.plusDays(1)) {
            long present, absent;

            if (date.isAfter(today)) {
                present = 0;
                absent = 0;
            } else {
                present = presentMap.getOrDefault(date, 0L);
                absent = totalEmployees - present;
            }

            Map<String, Object> record = new HashMap<>();
            record.put("date", date.toString());
            record.put("present", present);
            record.put("absent", absent);

            finalReport.add(record);
        }

        return finalReport;
    }

     public List<Map<String, Object>> getMonthlyReportForAllEmployees() {
        List<Map<String, Object>> report = new ArrayList<>();
        LocalDate now = LocalDate.now();

        for (int i = 0; i < 4; i++) {
            YearMonth yearMonth = YearMonth.from(now.minusMonths(i));
            LocalDate start = yearMonth.atDay(1);
            LocalDate end = yearMonth.atEndOfMonth();

            // Fetch all attendance records in that month
            List<Attendance> attendances = attendanceRepository.findByLoginTimeBetween(
                    Date.from(start.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                    Date.from(end.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toInstant())
            );

            // Group by employee and date
            Map<String, Map<LocalDate, List<Attendance>>> grouped = attendances.stream()
                    .collect(Collectors.groupingBy(
                            att -> att.getEmployee().getEmployeeId().toString(),
                            Collectors.groupingBy(
                                    att -> att.getLoginTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
                            )
                    ));

            long presentCount = 0;

            for (Map<LocalDate, List<Attendance>> empDays : grouped.values()) {
                for (List<Attendance> dayRecords : empDays.values()) {
                    boolean hasLogin = dayRecords.stream().anyMatch(a -> a.getAttendanceType().equalsIgnoreCase("Login"));
                    boolean hasLogout = dayRecords.stream().anyMatch(a -> a.getAttendanceType().equalsIgnoreCase("Logout"));

                    if (hasLogin && hasLogout) {
                        presentCount++;
                    }
                }
            }

            // Assume 22 working days per month for simplicity
            long totalWorkingDays = 22 * grouped.keySet().size();
            long absentCount = Math.max(0, totalWorkingDays - presentCount);

            Map<String, Object> monthlyData = new HashMap<>();
            monthlyData.put("month", yearMonth.getMonth().toString() + " " + yearMonth.getYear());
            monthlyData.put("present", presentCount);
            monthlyData.put("absent", absentCount);

            report.add(monthlyData);
        }

        return report;
    }
}
