package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Хранилище пользователей и связей дружбы.
 */
public interface UserStorage {

    User create(User user);

    void update(User user);

    void delete(Integer id);

    Optional<User> findById(Integer id);

    List<User> findAll();

    void addFriend(Integer userId, Integer friendId);

    void removeFriend(Integer userId, Integer friendId);

    List<User> getFriends(Integer userId);

    List<User> getCommonFriends(Integer userId, Integer otherId);
}
