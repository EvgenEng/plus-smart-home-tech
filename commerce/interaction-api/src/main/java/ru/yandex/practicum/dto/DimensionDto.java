package ru.yandex.practicum.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DimensionDto {
    @NotNull
    @Min(1)
    private Double width;

    @NotNull
    @Min(1)
    private Double height;

    @NotNull
    @Min(1)
    private Double depth;
}
