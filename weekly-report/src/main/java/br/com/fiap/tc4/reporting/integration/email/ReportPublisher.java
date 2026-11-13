package br.com.fiap.tc4.reporting.integration.email;

import br.com.fiap.tc4.reporting.model.ReportEmail;

@FunctionalInterface
public interface ReportPublisher {

    void publish(ReportEmail email);
}
