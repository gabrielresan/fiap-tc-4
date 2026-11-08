package com.example.feedbackservice.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /avaliacao}, with the field names defined by the Tech Challenge.
 */
public record FeedbackRequest(
        @NotBlank @Size(max = FeedbackRequest.MAX_DESCRIPTION_LENGTH) String descricao,
        @NotNull @Min(0) @Max(10) Integer nota
) {

    static final int MAX_DESCRIPTION_LENGTH = 2000;
}
