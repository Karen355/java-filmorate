package ru.yandex.practicum.filmorate.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReleaseDateValidator")
class ReleaseDateValidatorTest {

    private ReleaseDateValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReleaseDateValidator();
    }

    @Test
    @DisplayName("допускает дату 28 декабря 1895")
    void allowsFirstFilmDate() {
        assertThat(validator.isValid(LocalDate.of(1895, 12, 28), null)).isTrue();
    }

    @Test
    @DisplayName("допускает дату после 28 декабря 1895")
    void allowsDateAfterFirstFilm() {
        assertThat(validator.isValid(LocalDate.of(1896, 1, 1), null)).isTrue();
        assertThat(validator.isValid(LocalDate.now(), null)).isTrue();
    }

    @Test
    @DisplayName("отклоняет дату раньше 28 декабря 1895")
    void rejectsDateBeforeFirstFilm() {
        assertThat(validator.isValid(LocalDate.of(1895, 12, 27), null)).isFalse();
        assertThat(validator.isValid(LocalDate.of(1890, 1, 1), null)).isFalse();
    }

    @Test
    @DisplayName("допускает null (проверка на null в @NotNull)")
    void allowsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
