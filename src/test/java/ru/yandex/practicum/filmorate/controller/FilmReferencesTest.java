package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Рейтинг и жанры фильма в REST API")
class FilmReferencesTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Test
    @DisplayName("POST, GET и PUT: рейтинг и жанры возвращаются с именами и без дублей")
    void filmReferences_areNormalizedAndUpdated() throws Exception {
        ObjectNode film = validFilm();
        film.set("genres", objectMapper.readTree("[{\"id\":2},{\"id\":1},{\"id\":2,\"name\":\"Дубль\"}]"));
        MvcResult created = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mpa.name").value("G"))
                .andExpect(jsonPath("$.genres[*].id", contains(1, 2)))
                .andExpect(jsonPath("$.genres[*].name", contains("Комедия", "Драма")))
                .andReturn();
        int id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asInt();
        mockMvc.perform(get("/films/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mpa.name").value("G"))
                .andExpect(jsonPath("$.genres[*].id", contains(1, 2)));
        film.put("id", id);
        film.set("mpa", objectMapper.readTree("{\"id\":5,\"name\":\"Неверное имя\"}"));
        film.set("genres", objectMapper.readTree("[{\"id\":6}]"));

        mockMvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mpa.name").value("NC-17"))
                .andExpect(jsonPath("$.genres[*].id", contains(6)));
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mpa.id").value(5))
                .andExpect(jsonPath("$[0].genres[*].name", contains("Боевик")));
        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].genres[*].id", contains(6)));

        film.remove("genres");
        mockMvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").isEmpty());
        mockMvc.perform(get("/films/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genres").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"mpa", "genres"})
    @DisplayName("POST и PUT: неизвестные справочные id дают 404 и не меняют фильм")
    void unknownReference_returns404WithoutChanges(String field) throws Exception {
        ObjectNode film = validFilm();
        MvcResult created = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isCreated()).andReturn();
        int id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asInt();
        JsonNode invalid = objectMapper.readTree(field.equals("mpa") ? "{\"id\":999}" : "[{\"id\":999}]");
        film.set(field, invalid);

        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isNotFound());
        film.put("id", id);
        film.put("name", "Invalid update");
        mockMvc.perform(put("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Film"))
                .andExpect(jsonPath("$[0].mpa.id").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}"})
    @DisplayName("POST: отсутствующий рейтинг или его id дают 400")
    void missingMpa_returns400(String mpa) throws Exception {
        ObjectNode film = validFilm();
        film.set("mpa", objectMapper.readTree(mpa));

        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"[null]", "[{}]"})
    @DisplayName("POST: пустой элемент жанров или отсутствующий id дают 400")
    void malformedGenres_returns400(String genres) throws Exception {
        ObjectNode film = validFilm();
        film.set("genres", objectMapper.readTree(genres));

        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]"})
    @DisplayName("POST: null и пустой список жанров возвращаются как []")
    void emptyGenres_returnsEmptyArray(String genres) throws Exception {
        ObjectNode film = validFilm();
        film.set("genres", objectMapper.readTree(genres));

        mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON).content(film.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.genres").isArray())
                .andExpect(jsonPath("$.genres").isEmpty());
    }

    private ObjectNode validFilm() throws Exception {
        return (ObjectNode) objectMapper.readTree(
                "{\"name\":\"Film\",\"description\":\"Description\","
                        + "\"releaseDate\":\"2000-01-01\",\"duration\":100,"
                        + "\"mpa\":{\"id\":1}}");
    }
}
