package com.example.weeklyreport.publisher;

import com.example.weeklyreport.model.ReportEmail;

@FunctionalInterface
public interface ReportPublisher {

    void publish(ReportEmail email);
}
