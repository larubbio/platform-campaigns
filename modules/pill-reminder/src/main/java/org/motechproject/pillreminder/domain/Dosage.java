package org.motechproject.pillreminder.domain;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.motechproject.commons.date.model.Time;
import org.motechproject.commons.date.util.DateUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;


public class Dosage {
    private String id;
    private Time dosageTime;
    private LocalDate responseLastCapturedDate;
    private Set<Medicine> medicines;

    public Dosage() {
    }

    public Dosage(Time dosageTime, Set<Medicine> medicines) {
        this.id = UUID.randomUUID().toString();
        this.dosageTime = dosageTime;
        this.medicines = medicines;
    }

    public Set<Medicine> getMedicines() {
        return medicines;
    }

    public void setMedicines(Set<Medicine> medicines) {
        this.medicines = medicines;
    }

    public Time getDosageTime() {
        return dosageTime;
    }

    public void setDosageTime(Time dosageTime) {
        this.dosageTime = dosageTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public LocalDate getResponseLastCapturedDate() {
        return responseLastCapturedDate;
    }

    public void setResponseLastCapturedDate(LocalDate responseLastCapturedDate) {
        this.responseLastCapturedDate = responseLastCapturedDate;
    }

    public void updateResponseLastCapturedDate(LocalDate lastCapturedDate) {
        if (responseLastCapturedDate == null || responseLastCapturedDate.isBefore(lastCapturedDate)) {
            responseLastCapturedDate = lastCapturedDate;
        }
    }

    @JsonIgnore
    public boolean isTodaysDosageResponseCaptured() {
        LocalDate today = DateUtil.today();
        LocalDate yesterday = today.minusDays(1);
        LocalTime localNow = DateUtil.now().toLocalTime();
        if (responseLastCapturedDate == null) {
            return false;
        }
        if (responseLastCapturedDate.equals(today)) {
            return true;
        }
        return responseLastCapturedDate.equals(yesterday) && new Time(localNow.getHourOfDay(), localNow.getMinuteOfHour()).isBefore(dosageTime);
    }

    @JsonIgnore
    public LocalDate getStartDate() {
        List<Medicine> sortedList = new ArrayList<Medicine>(medicines);
        Collections.sort(sortedList, new Comparator<Medicine>() {
            @Override
            public int compare(Medicine o1, Medicine o2) {
                return o1.getStartDate().compareTo(o2.getStartDate());
            }
        });
        return sortedList.isEmpty() ? null : sortedList.get(0).getStartDate();
    }

    @JsonIgnore
    public LocalDate getEndDate() {
        Set<Medicine> medicinesWithNonNullEndDate = getMedicinesWithNonNullEndDate();
        if (medicinesWithNonNullEndDate.isEmpty()) {
            return null;
        }

        List<Medicine> sortedList = new ArrayList<Medicine>(medicinesWithNonNullEndDate);
        Collections.sort(sortedList, new Comparator<Medicine>() {
            @Override
            public int compare(Medicine o1, Medicine o2) {
                return o2.getEndDate().compareTo(o1.getEndDate());
            }
        });
        return sortedList.isEmpty() ? null : sortedList.get(0).getEndDate();
    }

    private Set<Medicine> getMedicinesWithNonNullEndDate() {
        Set<Medicine> medicinesWithNonNullEndDate = new HashSet<Medicine>();
        for (Medicine medicine : medicines) {
            if (medicine.getEndDate() != null) {
                medicinesWithNonNullEndDate.add(medicine);
            }
        }
        return medicinesWithNonNullEndDate;
    }

    public void validate() {
        for (Medicine medicine : getMedicines()) {
            medicine.validate();
        }
    }

    public DateTime todaysDosageTime() {
        return DateUtil.now().withHourOfDay(dosageTime.getHour()).withMinuteOfHour(dosageTime.getMinute());
    }
}
