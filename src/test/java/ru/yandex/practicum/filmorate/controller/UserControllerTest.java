package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
