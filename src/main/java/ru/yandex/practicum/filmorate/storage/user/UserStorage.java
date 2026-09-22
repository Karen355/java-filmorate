package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    Set<Integer> getFriendIds(Integer userId);
}
