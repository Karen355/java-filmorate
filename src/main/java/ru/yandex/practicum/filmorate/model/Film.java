package ru.yandex.practicum.filmorate.model;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.validation.ReleaseDate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Модель фильма.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Film {

    private Integer id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание не может превышать 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна")
    @ReleaseDate
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность обязательна")
    @Positive(message = "Продолжительность должна быть положительным числом")
    private Integer duration;

    @Valid
    @NotNull(message = "Рейтинг MPA обязателен")
    private Mpa mpa;

    @Builder.Default
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private List<@NotNull @Valid Genre> genres = new ArrayList<>();
}
