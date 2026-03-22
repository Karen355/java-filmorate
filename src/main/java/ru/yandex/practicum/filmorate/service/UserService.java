package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Бизнес-логика для пользователей: CRUD, друзья.
 */
@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;

    @Autowired
    public UserService(UserStorage userStorage) {
        this.userStorage = userStorage;
    }

    public User create(User user) {
        normalizeName(user);
        User created = userStorage.create(user);
        log.info("Создан пользователь: id={}, login={}", created.getId(), created.getLogin());
        return created;
    }

    public User update(User user) {
        if (user.getId() == null || userStorage.findById(user.getId()).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        normalizeName(user);
        userStorage.update(user);
        log.info("Обновлён пользователь: id={}, login={}", user.getId(), user.getLogin());
        return user;
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(Integer id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void addFriend(Integer userId, Integer friendId) {
        if (Objects.equals(userId, friendId)) {
            throw new ValidationException("Нельзя добавить самого себя в друзья");
        }
        ensureUserExists(userId);
        ensureUserExists(friendId);
        userStorage.addFriend(userId, friendId);
        log.info("Пользователь id={} добавил в друзья id={}", userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        ensureUserExists(userId);
        ensureUserExists(friendId);
        userStorage.removeFriend(userId, friendId);
        log.info("Пользователь id={} удалил из друзей id={}", userId, friendId);
    }

    public List<User> getFriends(Integer userId) {
        ensureUserExists(userId);
        return userStorage.getFriendIds(userId).stream()
                .sorted()
                .map(id -> userStorage.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        ensureUserExists(userId);
        ensureUserExists(otherId);
        Set<Integer> a = userStorage.getFriendIds(userId);
        Set<Integer> b = userStorage.getFriendIds(otherId);
        return a.stream()
                .filter(b::contains)
                .sorted()
                .map(id -> userStorage.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .toList();
    }

    private void ensureUserExists(Integer id) {
        if (userStorage.findById(id).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    private void normalizeName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
