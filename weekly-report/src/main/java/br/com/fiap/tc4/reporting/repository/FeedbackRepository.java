package br.com.fiap.tc4.reporting.repository;

import br.com.fiap.tc4.reporting.model.Feedback;
import br.com.fiap.tc4.reporting.model.ReportPeriod;

import java.util.List;

public interface FeedbackRepository {

    List<Feedback> findSubmittedWithin(ReportPeriod period);
}