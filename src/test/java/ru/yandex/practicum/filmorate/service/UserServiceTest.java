package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("addFriend: нельзя добавить самого себя")
    void addFriend_self_throwsValidationException() {
        assertThatThrownBy(() -> userService.addFriend(1, 1))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("самого себя");
    }

    @Test
    @DisplayName("addFriend: успешное добавление вызывает storage")
    void addFriend_ok_callsStorage() {
        when(userStorage.findById(1)).thenReturn(Optional.of(user(1)));
        when(userStorage.findById(2)).thenReturn(Optional.of(user(2)));

        userService.addFriend(1, 2);

        verify(userStorage).addFriend(1, 2);
    }

    private static User user(int id) {
        return User.builder()
                .id(id)
                .email(id + "@mail.ru")
                .login("u" + id)
                .name("User " + id)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
