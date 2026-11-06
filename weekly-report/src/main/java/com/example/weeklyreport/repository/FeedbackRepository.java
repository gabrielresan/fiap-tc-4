package com.example.weeklyreport.repository;

import com.example.weeklyreport.model.Feedback;
import com.example.weeklyreport.model.ReportPeriod;

import java.util.List;

public interface FeedbackRepository {

    List<Feedback> findSubmittedWithin(ReportPeriod period);
}