package br.com.fiap.tc4.reporting.publisher;

import br.com.fiap.tc4.reporting.model.ReportEmail;

@FunctionalInterface
public interface ReportPublisher {

    void publish(ReportEmail email);
}
