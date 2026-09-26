package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(GenreDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("GenreDbStorage")
class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Test
    @DisplayName("findAll: полный справочник с фиксированными id по возрастанию")
    void findAll_returnsReferenceData() {
        List<Genre> values = genreStorage.findAll();

        assertThat(values).extracting(Genre::getId)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, 6).boxed().toList());
        assertThat(values).extracting(Genre::getName)
                .containsExactly("Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик");
    }

    @Test
    @DisplayName("findById: все элементы справочника доступны по id")
    void findById_returnsReferenceData() {
        for (Genre value : genreStorage.findAll()) {
            assertThat(genreStorage.findById(value.getId())).contains(value);
        }
    }

    @Test
    @DisplayName("findById: неизвестный id возвращает Optional.empty")
    void findById_unknown_returnsEmpty() {
        assertThat(genreStorage.findById(999)).isEmpty();
        assertThat(genreStorage.findById(0)).isEmpty();
    }
}
