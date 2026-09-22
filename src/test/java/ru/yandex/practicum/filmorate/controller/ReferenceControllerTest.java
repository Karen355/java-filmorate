package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Справочники жанров и рейтингов")
class ReferenceControllerTest {

    private final MockMvc mockMvc;

    @Test
    @DisplayName("GET /mpa: ровно пять американских рейтингов с id от 1 до 5")
    void getMpa_returnsFiveRatings() throws Exception {
        mockMvc.perform(get("/mpa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5)))
                .andExpect(jsonPath("$[*].name", contains("G", "PG", "PG-13", "R", "NC-17")));
    }

    @Test
    @DisplayName("GET /genres: шесть жанров по возрастанию id")
    void getGenres_returnsSixGenres() throws Exception {
        mockMvc.perform(get("/genres"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5, 6)))
                .andExpect(jsonPath("$[*].name", contains(
                        "Комедия", "Драма", "Мультфильм", "Триллер", "Документальный", "Боевик")));
    }

    @Test
    @DisplayName("GET /mpa/{id} и /genres/{id}: объект с id и названием")
    void getReferenceById_returnsObject() throws Exception {
        mockMvc.perform(get("/mpa/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("PG-13"));
        mockMvc.perform(get("/genres/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Комедия"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/mpa/999", "/mpa/0", "/mpa/-1", "/genres/999", "/genres/0", "/genres/-1"})
    @DisplayName("Отсутствующий рейтинг или жанр: 404")
    void getReferenceById_unknown_returns404(String path) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());
    }
}
