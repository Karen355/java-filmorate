package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("FilmController")
class FilmControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    FilmControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    private String validFilmJson() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Фильм",
                "description", "Описание",
                "releaseDate", "1990-01-01",
                "duration", 120
        ));
    }

    @Test
    @DisplayName("POST /films - создаёт фильм при валидных данных")
    void addFilm_validBody_returnsCreated() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFilmJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Фильм"))
                .andExpect(jsonPath("$.duration").value(120));
    }

    @Test
    @DisplayName("POST /films - 400 при пустом названии")
    void addFilm_emptyName_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "",
                "description", "Описание",
                "releaseDate", "1990-01-01",
                "duration", 120
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films - 400 при описании длиннее 200 символов")
    void addFilm_descriptionTooLong_returnsBadRequest() throws Exception {
        String longDesc = "a".repeat(201);
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Фильм",
                "description", longDesc,
                "releaseDate", "1990-01-01",
                "duration", 120
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films - 400 при дате релиза раньше 28.12.1895")
    void addFilm_releaseDateBeforeLimit_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Фильм",
                "description", "Описание",
                "releaseDate", "1895-12-27",
                "duration", 120
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films - 400 при неположительной продолжительности")
    void addFilm_nonPositiveDuration_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "name", "Фильм",
                "description", "Описание",
                "releaseDate", "1990-01-01",
                "duration", 0
        ));
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /films - 400 при пустом теле запроса")
    void addFilm_emptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /films - возвращает список фильмов")
    void getAllFilms_returnsOk() throws Exception {
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /films/popular - возвращает список")
    void getPopular_returnsOk() throws Exception {
        mockMvc.perform(get("/films/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /films/{id} - возвращает фильм после создания")
    void getFilmById_returnsOk() throws Exception {
        MvcResult created = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFilmJson()))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode root = objectMapper.readTree(created.getResponse().getContentAsString());
        int filmId = root.get("id").asInt();
        mockMvc.perform(get("/films/" + filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(filmId))
                .andExpect(jsonPath("$.name").value("Фильм"));
    }

    @Test
    @DisplayName("PUT /films/{id}/like/{userId} - лайк (после создания пользователя и фильма)")
    void addLike_returnsOk() throws Exception {
        MvcResult userResult = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "like-user@mail.ru",
                                "login", "likeuser",
                                "birthday", "1990-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        int userId = objectMapper.readTree(userResult.getResponse().getContentAsString()).get("id").asInt();
        MvcResult filmResult = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validFilmJson()))
                .andExpect(status().isCreated())
                .andReturn();
        int filmId = objectMapper.readTree(filmResult.getResponse().getContentAsString()).get("id").asInt();
        mockMvc.perform(put("/films/" + filmId + "/like/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /films - 404 при обновлении несуществующего фильма")
    void updateFilm_notFound_returns404() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "id", 99999,
                "name", "Фильм",
                "description", "Описание",
                "releaseDate", "1990-01-01",
                "duration", 120
        ));
        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }
}
