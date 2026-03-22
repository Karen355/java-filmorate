package ru.yandex.practicum.filmorate.storage.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory реализация хранилища пользователей и дружбы (симметрично, id друзей в Set).
 */
@Component
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> friends = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    @Override
    public User create(User user) {
        user.setId(idGenerator.getAndIncrement());
        users.put(user.getId(), user);
        friends.putIfAbsent(user.getId(), ConcurrentHashMap.newKeySet());
        return user;
    }

    @Override
    public void update(User user) {
        users.put(user.getId(), user);
        friends.putIfAbsent(user.getId(), ConcurrentHashMap.newKeySet());
    }

    @Override
    public void delete(Integer id) {
        users.remove(id);
        Set<Integer> removedFriends = friends.remove(id);
        if (removedFriends != null) {
            for (Integer friendId : removedFriends) {
                Set<Integer> friendSet = friends.get(friendId);
                if (friendSet != null) {
                    friendSet.remove(id);
                }
            }
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        friends.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(friendId);
        friends.computeIfAbsent(friendId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        Set<Integer> a = friends.get(userId);
        Set<Integer> b = friends.get(friendId);
        if (a != null) {
            a.remove(friendId);
        }
        if (b != null) {
            b.remove(userId);
        }
    }

    @Override
    public Set<Integer> getFriendIds(Integer userId) {
        Set<Integer> set = friends.get(userId);
        return set == null ? Collections.emptySet() : Set.copyOf(set);
    }
}
