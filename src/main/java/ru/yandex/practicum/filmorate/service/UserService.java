package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Objects;

/**
 * Бизнес-логика для пользователей: CRUD, друзья.
 */
@Slf4j
@Service
public class UserService {

    private final UserStorage userStorage;
    private final FeedStorage feedStorage;

    @Autowired
    public UserService(UserStorage userStorage, FeedStorage feedStorage) {
        this.userStorage = userStorage;
        this.feedStorage = feedStorage;
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

    public void delete(Integer id) {
        ensureUserExists(id);
        userStorage.delete(id);
        log.info("Удалён пользователь: id={}", id);
    }

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(Integer id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    public void addFriend(Integer userId, Integer friendId) {
        ensureDifferentUsers(userId, friendId);
        ensureUserExists(userId);
        ensureUserExists(friendId);
        if (userStorage.addFriend(userId, friendId)) {
            feedStorage.addEvent(userId, EventType.FRIEND, Operation.ADD, friendId);
        }
        log.info("Пользователь id={} добавил в друзья id={}", userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        ensureDifferentUsers(userId, friendId);
        ensureUserExists(userId);
        ensureUserExists(friendId);
        if (userStorage.removeFriend(userId, friendId)) {
            feedStorage.addEvent(userId, EventType.FRIEND, Operation.REMOVE, friendId);
        }
        log.info("Пользователь id={} удалил из друзей id={}", userId, friendId);
    }

    public List<User> getFriends(Integer userId) {
        ensureUserExists(userId);
        return userStorage.getFriends(userId);
    }

    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        ensureDifferentUsers(userId, otherId);
        ensureUserExists(userId);
        ensureUserExists(otherId);
        return userStorage.getCommonFriends(userId, otherId);
    }

    public List<FeedEvent> getFeed(Integer userId) {
        ensureUserExists(userId);
        return feedStorage.getFeed(userId);
    }

    private void ensureUserExists(Integer id) {
        if (userStorage.findById(id).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + id + " не найден");
        }
    }

    private void ensureDifferentUsers(Integer userId, Integer otherUserId) {
        if (Objects.equals(userId, otherUserId)) {
            throw new ValidationException("Операция с одним и тем же пользователем недопустима");
        }
    }

    private void normalizeName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
