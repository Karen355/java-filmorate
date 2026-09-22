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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("UserController")
class UserControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    UserControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    private String validUserJson() throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "email", "user@mail.ru",
                "login", "user",
                "birthday", "1990-01-01"
        ));
    }

    @Test
    @DisplayName("POST /users - создаёт пользователя при валидных данных")
    void createUser_validBody_returnsCreated() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("user@mail.ru"))
                .andExpect(jsonPath("$.login").value("user"));
    }

    @Test
    @DisplayName("POST /users - 400 при пустом email")
    void createUser_emptyEmail_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "",
                "login", "user",
                "birthday", "1990-01-01"
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - 400 при email без @")
    void createUser_invalidEmail_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "invalid-email",
                "login", "user",
                "birthday", "1990-01-01"
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - 400 при пустом логине")
    void createUser_emptyLogin_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "user@mail.ru",
                "login", "",
                "birthday", "1990-01-01"
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - 400 при логине с пробелами")
    void createUser_loginWithSpaces_returnsBadRequest() throws Exception {
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "user@mail.ru",
                "login", "user name",
                "birthday", "1990-01-01"
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - 400 при дате рождения в будущем")
    void createUser_futureBirthday_returnsBadRequest() throws Exception {
        String futureDate = LocalDate.now().plusDays(1).toString();
        String json = objectMapper.writeValueAsString(java.util.Map.of(
                "email", "user@mail.ru",
                "login", "user",
                "birthday", futureDate
        ));
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /users - при пустом name подставляется login")
    void createUser_emptyName_usesLoginAsName() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("user"));
    }

    @Test
    @DisplayName("POST /users - 400 при пустом теле запроса")
    void createUser_emptyBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /users - возвращает список пользователей")
    void getAllUsers_returnsOk() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /users/{id} - 404 если пользователь не найден")
    void getUserById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/users/99999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /users/{id} - возвращает пользователя после создания")
    void getUserById_returnsOk() throws Exception {
        MvcResult created = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validUserJson()))
                .andExpect(status().isCreated())
                .andReturn();
        int id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asInt();
        mockMvc.perform(get("/users/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.login").value("user"));
    }

    @Test
    @DisplayName("Друзья: PUT и GET /users/{id}/friends")
    void addFriend_and_getFriends_returnsOk() throws Exception {
        MvcResult u1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "a@mail.ru",
                                "login", "a",
                                "birthday", "1990-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult u2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "b@mail.ru",
                                "login", "b",
                                "birthday", "1991-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode n1 = objectMapper.readTree(u1.getResponse().getContentAsString());
        JsonNode n2 = objectMapper.readTree(u2.getResponse().getContentAsString());
        int id1 = n1.get("id").asInt();
        int id2 = n2.get("id").asInt();
        mockMvc.perform(put("/users/" + id1 + "/friends/" + id2))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/" + id1 + "/friends"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id2));
    }

    @Test
    @DisplayName("GET /users/{id}/friends/common/{otherId} - общие друзья")
    void getCommonFriends_returnsOk() throws Exception {
        MvcResult u1 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "c1@mail.ru",
                                "login", "c1",
                                "birthday", "1990-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult u2 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "c2@mail.ru",
                                "login", "c2",
                                "birthday", "1991-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult u3 = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "c3@mail.ru",
                                "login", "c3",
                                "birthday", "1992-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        int id1 = objectMapper.readTree(u1.getResponse().getContentAsString()).get("id").asInt();
        int id2 = objectMapper.readTree(u2.getResponse().getContentAsString()).get("id").asInt();
        int id3 = objectMapper.readTree(u3.getResponse().getContentAsString()).get("id").asInt();
        mockMvc.perform(put("/users/" + id1 + "/friends/" + id3)).andExpect(status().isOk());
        mockMvc.perform(put("/users/" + id2 + "/friends/" + id3)).andExpect(status().isOk());
        mockMvc.perform(get("/users/" + id1 + "/friends/common/" + id2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id3));
    }

    @Test
    @DisplayName("PUT /users/{id}/friends/{friendId} - нельзя добавить самого себя")
    void addFriend_self_returns400() throws Exception {
        MvcResult u = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "email", "self@mail.ru",
                                "login", "selfuser",
                                "birthday", "1990-01-01"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        int id = objectMapper.readTree(u.getResponse().getContentAsString()).get("id").asInt();
        mockMvc.perform(put("/users/" + id + "/friends/" + id))
                .andExpect(status().isBadRequest());
    }
}
