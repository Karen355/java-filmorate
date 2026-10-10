package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("ReviewController")
class ReviewControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private int userId;
    private int filmId;

    @Autowired
    ReviewControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void setUp() throws Exception {
        userId = createUser("reviewer");
        filmId = createId("/films", Map.of(
                "name", "Фильм",
                "description", "Описание",
                "releaseDate", "1990-01-01",
                "duration", 120,
                "mpa", Map.of("id", 1)), "id");
    }

    @Test
    @DisplayName("POST /reviews - создаёт отзыв с нулевой полезностью")
    void create_validBody_returnsCreated() throws Exception {
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").isNumber())
                .andExpect(jsonPath("$.content").value("This film is sooo baad."))
                .andExpect(jsonPath("$.isPositive").value(false))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.filmId").value(filmId))
                .andExpect(jsonPath("$.useful").value(0));
    }

    @Test
    @DisplayName("POST /reviews - 400 без обязательных полей, 404 для несуществующих пользователя и фильма")
    void create_invalidBody_returnsError() throws Exception {
        for (String field : new String[]{"content", "isPositive", "userId", "filmId"}) {
            Map<String, Object> body = reviewBody();
            body.remove(field);
            mockMvc.perform(post("/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isBadRequest());
        }
        Map<String, Object> unknownUser = reviewBody();
        unknownUser.put("userId", -1);
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownUser)))
                .andExpect(status().isNotFound());
        Map<String, Object> unknownFilm = reviewBody();
        unknownFilm.put("filmId", -1);
        mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownFilm)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /reviews - обновляет текст и тип, автор и фильм не меняются")
    void update_changesContentAndType() throws Exception {
        int reviewId = createReview();
        int otherUserId = createUser("other");
        Map<String, Object> body = reviewBody();
        body.put("reviewId", reviewId);
        body.put("content", "Передумал, хороший фильм");
        body.put("isPositive", true);
        body.put("userId", otherUserId);

        mockMvc.perform(put("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(reviewId))
                .andExpect(jsonPath("$.content").value("Передумал, хороший фильм"))
                .andExpect(jsonPath("$.isPositive").value(true))
                .andExpect(jsonPath("$.userId").value(userId));

        body.put("reviewId", 999999);
        mockMvc.perform(put("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("лайк, дизлайк и их удаление меняют полезность отзыва")
    void likesAndDislikes_changeUseful() throws Exception {
        int reviewId = createReview();
        int otherUserId = createUser("other");

        mockMvc.perform(put("/reviews/" + reviewId + "/like/" + userId)).andExpect(status().isOk());
        mockMvc.perform(put("/reviews/" + reviewId + "/like/" + otherUserId)).andExpect(status().isOk());
        mockMvc.perform(get("/reviews/" + reviewId)).andExpect(jsonPath("$.useful").value(2));

        mockMvc.perform(put("/reviews/" + reviewId + "/dislike/" + otherUserId)).andExpect(status().isOk());
        mockMvc.perform(get("/reviews/" + reviewId)).andExpect(jsonPath("$.useful").value(0));

        mockMvc.perform(delete("/reviews/" + reviewId + "/dislike/" + otherUserId)).andExpect(status().isOk());
        mockMvc.perform(delete("/reviews/" + reviewId + "/like/" + userId)).andExpect(status().isOk());
        mockMvc.perform(get("/reviews/" + reviewId)).andExpect(jsonPath("$.useful").value(0));

        mockMvc.perform(put("/reviews/" + reviewId + "/like/999999")).andExpect(status().isNotFound());
        mockMvc.perform(put("/reviews/999999/like/" + userId)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /reviews - сортировка по полезности, фильтр по фильму и count")
    void findAll_sortsByUseful() throws Exception {
        int first = createReview();
        int second = createReview();
        mockMvc.perform(put("/reviews/" + second + "/like/" + userId)).andExpect(status().isOk());

        mockMvc.perform(get("/reviews").param("filmId", String.valueOf(filmId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].reviewId").value(second))
                .andExpect(jsonPath("$[1].reviewId").value(first));
        mockMvc.perform(get("/reviews").param("filmId", String.valueOf(filmId)).param("count", "1"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].reviewId").value(second));
        mockMvc.perform(get("/reviews").param("count", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE /reviews/{id} - удаляет отзыв, повторно - 404")
    void delete_removesReview() throws Exception {
        int reviewId = createReview();

        mockMvc.perform(delete("/reviews/" + reviewId)).andExpect(status().isOk());
        mockMvc.perform(get("/reviews/" + reviewId)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/reviews/" + reviewId)).andExpect(status().isNotFound());
    }

    private Map<String, Object> reviewBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("content", "This film is sooo baad.");
        body.put("isPositive", false);
        body.put("userId", userId);
        body.put("filmId", filmId);
        return body;
    }

    private int createReview() throws Exception {
        return createId("/reviews", reviewBody(), "reviewId");
    }

    private int createUser(String login) throws Exception {
        return createId("/users", Map.of(
                "email", login + "@mail.ru",
                "login", login,
                "birthday", "1990-01-01"), "id");
    }

    private int createId(String path, Map<String, Object> body, String idField) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(idField).asInt();
    }
}
