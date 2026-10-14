
package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Recommendations")
class RecommendationControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    RecommendationControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    private int createUser(String login) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "email", login + "@mail.ru",
                "login", login,
                "birthday", "1990-01-01"
        ));

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asInt();
    }

    private int createFilm(String name) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "name", name,
                "description", "Тестовый фильм",
                "releaseDate", "2000-01-01",
                "duration", 120,
                "mpa", Map.of("id", 1)
        ));

        MvcResult result = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(
                result.getResponse().getContentAsString()
        ).get("id").asInt();
    }

    private void addLike(int userId, int filmId) throws Exception {
        mockMvc.perform(put(
                        "/films/{filmId}/like/{userId}",
                        filmId, userId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Рекомендации от наиболее похожего пользователя")
    void recommendations_returnsSimilarUserFilms() throws Exception {
        int user1 = createUser("recUser1");
        int user2 = createUser("recUser2");
        int user3 = createUser("recUser3");

        int film1 = createFilm("Фильм 1");
        int film2 = createFilm("Фильм 2");
        int film3 = createFilm("Фильм 3");
        int film4 = createFilm("Фильм 4");

        addLike(user1, film1);
        addLike(user1, film2);

        addLike(user2, film1);
        addLike(user2, film2);
        addLike(user2, film3);

        addLike(user3, film1);
        addLike(user3, film4);

        mockMvc.perform(get(
                        "/users/{id}/recommendations", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(film3));
    }

    @Test
    @DisplayName("Нет общих лайков - пустой список")
    void recommendations_noCommonLikes_returnsEmpty() throws Exception {
        int user1 = createUser("recUser4");
        int user2 = createUser("recUser5");

        int film1 = createFilm("Фильм 5");
        int film2 = createFilm("Фильм 6");

        addLike(user1, film1);
        addLike(user2, film2);

        mockMvc.perform(get(
                        "/users/{id}/recommendations", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Пользователь не найден - 404")
    void recommendations_userNotFound_returns404() throws Exception {
        mockMvc.perform(get("/users/9999999/recommendations"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Все фильмы уже понравились - пустой список")
    void recommendations_allFilmsLiked_returnsEmpty() throws Exception {
        int user1 = createUser("recUser6");
        int user2 = createUser("recUser7");

        int film1 = createFilm("Фильм 7");

        addLike(user1, film1);
        addLike(user2, film1);

        mockMvc.perform(get(
                        "/users/{id}/recommendations", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }


    @Test
    @DisplayName("Рекомендации от двух одинаково похожих пользователей")
    void recommendations_equalSimilarity_returnsBothFilms() throws Exception {

        int user1 = createUser("recTieUser1");
        int user2 = createUser("recTieUser2");
        int user3 = createUser("recTieUser3");

        int film1 = createFilm("Фильм 1");
        int film2 = createFilm("Фильм 2");
        int film3 = createFilm("Фильм 3");
        int film4 = createFilm("Фильм 4");

        addLike(user1, film1);
        addLike(user1, film2);

        addLike(user2, film1);
        addLike(user2, film3);

        addLike(user3, film2);
        addLike(user3, film4);

        mockMvc.perform(get(
                        "/users/{id}/recommendations", user1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].id",
                        containsInAnyOrder(film3, film4)));
    }

}
