package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("UserDbStorage")
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Test
    @DisplayName("create и findById: сохраняются все поля, id выдаёт база")
    void createAndFindById_persistsUser() {
        User user = user("first");
        user.setId(99999);

        User created = userStorage.create(user);

        assertThat(created.getId()).isPositive().isNotEqualTo(99999);
        assertThat(userStorage.findById(created.getId())).contains(created);
    }

    @Test
    @DisplayName("findById: отсутствующий пользователь возвращает Optional.empty")
    void findById_unknown_returnsEmpty() {
        assertThat(userStorage.findById(-1)).isEmpty();
    }

    @Test
    @DisplayName("findAll: пустое хранилище и сортировка пользователей по id")
    void findAll_returnsUsersInIdOrder() {
        assertThat(userStorage.findAll()).isEmpty();
        User first = userStorage.create(user("first"));
        User second = userStorage.create(user("second"));

        assertThat(userStorage.findAll()).containsExactly(first, second);
    }

    @Test
    @DisplayName("update: обновляет поля, сохраняет id и друзей")
    void update_changesFieldsAndPreservesFriends() {
        User first = userStorage.create(user("first"));
        User second = userStorage.create(user("second"));
        userStorage.addFriend(first.getId(), second.getId());
        User updated = user("updated");
        updated.setId(first.getId());
        updated.setBirthday(LocalDate.of(2001, 2, 3));

        userStorage.update(updated);

        assertThat(userStorage.findById(first.getId())).contains(updated);
        assertThat(userStorage.getFriendIds(first.getId())).containsExactly(second.getId());
        assertThat(userStorage.findById(second.getId())).contains(second);
    }

    @Test
    @DisplayName("update: отсутствующий пользователь не создаётся")
    void update_unknown_throwsNotFound() {
        User user = user("unknown");
        user.setId(-1);

        assertThatThrownBy(() -> userStorage.update(user)).isInstanceOf(NotFoundException.class);
        assertThat(userStorage.findAll()).isEmpty();
    }

    @Test
    @DisplayName("addFriend: односторонняя заявка, повтор и ответное подтверждение")
    void addFriend_isDirectedAndIdempotent() {
        User first = userStorage.create(user("first"));
        User second = userStorage.create(user("second"));
        userStorage.addFriend(first.getId(), second.getId());
        userStorage.addFriend(first.getId(), second.getId());

        assertThat(userStorage.getFriendIds(first.getId())).containsExactly(second.getId());
        assertThat(userStorage.getFriendIds(second.getId())).isEmpty();

        userStorage.addFriend(second.getId(), first.getId());

        assertThat(userStorage.getFriendIds(first.getId())).containsExactly(second.getId());
        assertThat(userStorage.getFriendIds(second.getId())).containsExactly(first.getId());
    }

    @Test
    @DisplayName("removeFriend: удаляет только исходящую связь, повтор безопасен")
    void removeFriend_preservesReverseFriendship() {
        User first = userStorage.create(user("first"));
        User second = userStorage.create(user("second"));
        userStorage.addFriend(first.getId(), second.getId());
        userStorage.addFriend(second.getId(), first.getId());

        userStorage.removeFriend(first.getId(), second.getId());
        userStorage.removeFriend(first.getId(), second.getId());

        assertThat(userStorage.getFriendIds(first.getId())).isEmpty();
        assertThat(userStorage.getFriendIds(second.getId())).containsExactly(first.getId());
    }

    @Test
    @DisplayName("delete: удаляет пользователя и все входящие и исходящие связи")
    void delete_removesUserAndFriendships() {
        User first = userStorage.create(user("first"));
        User second = userStorage.create(user("second"));
        User third = userStorage.create(user("third"));
        userStorage.addFriend(first.getId(), second.getId());
        userStorage.addFriend(third.getId(), first.getId());
        userStorage.addFriend(second.getId(), third.getId());

        userStorage.delete(first.getId());
        userStorage.delete(first.getId());

        assertThat(userStorage.findById(first.getId())).isEmpty();
        assertThat(userStorage.getFriendIds(first.getId())).isEmpty();
        assertThat(userStorage.getFriendIds(third.getId())).isEmpty();
        assertThat(userStorage.getFriendIds(second.getId())).containsExactly(third.getId());
        assertThat(userStorage.findAll()).containsExactly(second, third);
    }

    private User user(String login) {
        return User.builder()
                .email(login + "@mail.ru")
                .login(login)
                .name("User " + login)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
