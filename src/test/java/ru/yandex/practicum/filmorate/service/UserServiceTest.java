package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
                .hasMessageContaining("одним и тем же");
    }

    @Test
    @DisplayName("addFriend: успешное добавление вызывает storage")
    void addFriend_ok_callsStorage() {
        when(userStorage.findById(1)).thenReturn(Optional.of(user(1)));
        when(userStorage.findById(2)).thenReturn(Optional.of(user(2)));

        userService.addFriend(1, 2);

        verify(userStorage).addFriend(1, 2);
    }

    @Test
    @DisplayName("removeFriend: нельзя удалить самого себя из друзей")
    void removeFriend_self_throwsValidationException() {
        assertThatThrownBy(() -> userService.removeFriend(1, 1))
                .isInstanceOf(ValidationException.class);
        verify(userStorage, never()).removeFriend(1, 1);
    }

    @Test
    @DisplayName("getCommonFriends: одинаковые id - ValidationException")
    void getCommonFriends_sameIds_throwsValidationException() {
        assertThatThrownBy(() -> userService.getCommonFriends(1, 1))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("getFriends: возвращает готовый список без загрузки каждого друга по id")
    void getFriends_returnsUsersFromStorage() {
        User friend = user(2);
        when(userStorage.findById(1)).thenReturn(Optional.of(user(1)));
        when(userStorage.getFriends(1)).thenReturn(List.of(friend));

        assertThat(userService.getFriends(1)).containsExactly(friend);
        verify(userStorage).findById(1);
        verify(userStorage).getFriends(1);
        verifyNoMoreInteractions(userStorage);
    }

    @Test
    @DisplayName("getFriends: неизвестный пользователь - NotFoundException")
    void getFriends_unknownUser_throwsNotFound() {
        when(userStorage.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getFriends(1)).isInstanceOf(NotFoundException.class);
        verify(userStorage, never()).getFriends(1);
    }

    @Test
    @DisplayName("getCommonFriends: возвращает результат DAO без загрузки отдельных друзей")
    void getCommonFriends_returnsUsersFromStorage() {
        User common = user(3);
        when(userStorage.findById(1)).thenReturn(Optional.of(user(1)));
        when(userStorage.findById(2)).thenReturn(Optional.of(user(2)));
        when(userStorage.getCommonFriends(1, 2)).thenReturn(List.of(common));

        assertThat(userService.getCommonFriends(1, 2)).containsExactly(common);
        verify(userStorage).findById(1);
        verify(userStorage).findById(2);
        verify(userStorage).getCommonFriends(1, 2);
        verifyNoMoreInteractions(userStorage);
    }

    @Test
    @DisplayName("getCommonFriends: неизвестный первый пользователь - NotFoundException")
    void getCommonFriends_unknownUser_throwsNotFound() {
        when(userStorage.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCommonFriends(1, 2)).isInstanceOf(NotFoundException.class);
        verify(userStorage, never()).getCommonFriends(1, 2);
    }

    @Test
    @DisplayName("getCommonFriends: неизвестный второй пользователь - NotFoundException")
    void getCommonFriends_unknownOtherUser_throwsNotFound() {
        when(userStorage.findById(1)).thenReturn(Optional.of(user(1)));
        when(userStorage.findById(2)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCommonFriends(1, 2)).isInstanceOf(NotFoundException.class);
        verify(userStorage, never()).getCommonFriends(1, 2);
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
