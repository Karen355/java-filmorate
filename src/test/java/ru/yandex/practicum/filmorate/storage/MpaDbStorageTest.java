package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(MpaDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("MpaDbStorage")
class MpaDbStorageTest {

    private final MpaDbStorage mpaStorage;

    @Test
    @DisplayName("findAll: полный справочник с фиксированными id по возрастанию")
    void findAll_returnsReferenceData() {
        List<Mpa> values = mpaStorage.findAll();

        assertThat(values).extracting(Mpa::getId)
                .containsExactlyElementsOf(IntStream.rangeClosed(1, 5).boxed().toList());
        assertThat(values).extracting(Mpa::getName)
                .containsExactly("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    @DisplayName("findById: все элементы справочника доступны по id")
    void findById_returnsReferenceData() {
        for (Mpa value : mpaStorage.findAll()) {
            assertThat(mpaStorage.findById(value.getId())).contains(value);
        }
    }

    @Test
    @DisplayName("findById: неизвестный id возвращает Optional.empty")
    void findById_unknown_returnsEmpty() {
        assertThat(mpaStorage.findById(999)).isEmpty();
        assertThat(mpaStorage.findById(0)).isEmpty();
    }
}
