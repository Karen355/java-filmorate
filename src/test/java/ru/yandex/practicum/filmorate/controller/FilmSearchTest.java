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

import java.util.List;
import java.util.Map;

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
@DisplayName("Поиск фильмов")
class FilmSearchTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Test
    @DisplayName("Поиск по названию и режиссёру возвращает фильмы по популярности без дублей")
    void searchByTitleAndDirector_sortsByLikes() throws Exception {
        int matchingDirector = createDirector("Крадов");
        int otherDirector = createDirector("Другой");
        int titleFilm = createFilm("Крадущийся тигр", otherDirector);
        int directorFilm = createFilm("Ночной фильм", matchingDirector);
        int bothFilm = createFilm("Крад в ночи", matchingDirector);
        createFilm("Посторонний фильм", otherDirector);
        int firstUser = createUser("first");
        int secondUser = createUser("second");
        int thirdUser = createUser("third");
        addLike(titleFilm, firstUser);
        addLike(titleFilm, secondUser);
        addLike(directorFilm, firstUser);
        addLike(bothFilm, firstUser);
        addLike(bothFilm, secondUser);
        addLike(bothFilm, thirdUser);

        mockMvc.perform(get("/films/search").param("query", "кРаД").param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(bothFilm, titleFilm)));
        mockMvc.perform(get("/films/search").param("query", "кРаД").param("by", "director"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(bothFilm, directorFilm)));
        mockMvc.perform(get("/films/search").param("query", "кРаД").param("by", "director,title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(bothFilm, titleFilm, directorFilm)))
                .andExpect(jsonPath("$[0].directors[0].name").value("Крадов"))
                .andExpect(jsonPath("$[0].genres[0].name").value("Комедия"));
    }

    @Test
    @DisplayName("Пустой запрос возвращает пустой список, неверное поле поиска даёт 400")
    void invalidSearchParameters_areHandled() throws Exception {
        int directorId = createDirector("Режиссёр");
        createFilm("Фильм", directorId);

        mockMvc.perform(get("/films/search").param("query", " ").param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/films/search").param("query", "Фильм").param("by", "genre"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/films/search").param("query", "Фильм"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/films/search").param("by", "title"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Символы SQL-шаблона ищутся как обычный текст")
    void searchTreatsWildcardsAsText() throws Exception {
        int directorId = createDirector("Режиссёр");
        int percentFilm = createFilm("100% кино", directorId);
        createFilm("Обычное кино", directorId);

        mockMvc.perform(get("/films/search").param("query", "%").param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", contains(percentFilm)));
    }

    private int createDirector(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private int createFilm(String name, int directorId) throws Exception {
        Map<String, Object> film = Map.of(
                "name", name,
                "description", "Описание",
                "releaseDate", "2000-01-01",
                "duration", 100,
                "mpa", Map.of("id", 1),
                "genres", List.of(Map.of("id", 1)),
                "directors", List.of(Map.of("id", directorId))
        );
        MvcResult result = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private int createUser(String login) throws Exception {
        Map<String, Object> user = Map.of(
                "email", login + "@mail.ru",
                "login", login,
                "birthday", "1990-01-01"
        );
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asInt();
    }

    private void addLike(int filmId, int userId) throws Exception {
        mockMvc.perform(put("/films/{id}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());
    }
}
