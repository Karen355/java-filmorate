package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("Режиссёры в REST API")
class DirectorControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Test
    @DisplayName("Создание, получение, обновление и удаление режиссёра")
    void directorCrud() throws Exception {
        int id = createDirector("Первый режиссёр");

        mockMvc.perform(get("/directors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(id)));
        mockMvc.perform(get("/directors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Первый режиссёр"));
        mockMvc.perform(put("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"name\":\"Новое имя\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое имя"));
        mockMvc.perform(delete("/directors/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/directors/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Фильмы хранят режиссёров и сортируются по году или числу лайков")
    void filmsByDirector_areSortedAndLoaded() throws Exception {
        int directorId = createDirector("Режиссёр");
        int otherDirectorId = createDirector("Другой режиссёр");
        int firstId = createFilm("Первый", "2001-01-01", directorId);
        int secondId = createFilm("Второй", "1999-01-01", directorId);
        int thirdId = createFilm("Третий", "2005-01-01", directorId);
        createFilm("Чужой", "2000-01-01", otherDirectorId);
        int firstUserId = createUser("first");
        int secondUserId = createUser("second");
        mockMvc.perform(put("/films/{id}/like/{userId}", firstId, firstUserId)).andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/like/{userId}", firstId, secondUserId)).andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/like/{userId}", thirdId, firstUserId)).andExpect(status().isOk());

        mockMvc.perform(get("/films/director/{id}", directorId).param("sortBy", "year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(secondId, firstId, thirdId)))
                .andExpect(jsonPath("$[0].directors[0].name").value("Режиссёр"));
        mockMvc.perform(get("/films/director/{id}", directorId).param("sortBy", "likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(firstId, thirdId, secondId)));

        mockMvc.perform(get("/films/{id}", firstId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directors[0].id").value(directorId));
        mockMvc.perform(get("/films/director/{id}", directorId).param("sortBy", "unknown"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/films/director/999999").param("sortBy", "year"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление фильма заменяет режиссёров, удаление режиссёра сохраняет фильм")
    void filmDirectors_areReplacedAndDeleted() throws Exception {
        int firstDirectorId = createDirector("Первый");
        int secondDirectorId = createDirector("Второй");
        int filmId = createFilm("Фильм", "2000-01-01", firstDirectorId);

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson("Фильм", "2000-01-01", secondDirectorId, filmId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directors[0].id").value(secondDirectorId));
        mockMvc.perform(get("/films/director/{id}", firstDirectorId).param("sortBy", "year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(delete("/directors/{id}", secondDirectorId)).andExpect(status().isOk());
        mockMvc.perform(get("/films/{id}", filmId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.directors").isEmpty());
    }

    @Test
    @DisplayName("Неизвестный режиссёр фильма даёт 404, пустое имя даёт 400")
    void invalidDirector_isRejected() throws Exception {
        mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson("Фильм", "2000-01-01", 999999, null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/films")).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Поле director из задания принимается, ответ содержит directors")
    void singularDirectorField_isAccepted() throws Exception {
        int directorId = createDirector("Режиссёр");
        String film = filmJson("Фильм", "2000-01-01", directorId, null)
                .replace("\"directors\":", "\"director\":");

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(film))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.directors[0].id").value(directorId));
    }

    private int createDirector(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new NameRequest(name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private int createFilm(String name, String releaseDate, int directorId) throws Exception {
        MvcResult result = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson(name, releaseDate, directorId, null)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private int createUser(String login) throws Exception {
        String json = "{\"email\":\"" + login + "@mail.ru\",\"login\":\"" + login
                + "\",\"birthday\":\"1990-01-01\"}";
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private String filmJson(String name, String releaseDate, int directorId, Integer filmId) {
        String id = filmId == null ? "" : "\"id\":" + filmId + ",";
        return "{" + id + "\"name\":\"" + name + "\",\"description\":\"Описание\","
                + "\"releaseDate\":\"" + releaseDate + "\",\"duration\":100,"
                + "\"mpa\":{\"id\":1},\"directors\":[{\"id\":" + directorId + "}]}";
    }

    private record NameRequest(String name) {
    }
}
