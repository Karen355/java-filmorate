package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;
    private final UserRowMapper rowMapper = new UserRowMapper();

    @Override
    public User create(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getLogin());
            statement.setString(3, user.getName());
            statement.setObject(4, user.getBirthday());
            return statement;
        }, keyHolder);
        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return user;
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, user.getEmail(), user.getLogin(), user.getName(),
                user.getBirthday(), user.getId());
        if (updated == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public Optional<User> findById(Integer id) {
        return jdbcTemplate.query("SELECT id, email, login, name, birthday FROM users WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<User> findAll() {
        return jdbcTemplate.query("SELECT id, email, login, name, birthday FROM users ORDER BY id", rowMapper);
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update("MERGE INTO friendships (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)",
                userId, friendId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ? AND friend_id = ?", userId, friendId);
    }

    @Override
    public List<User> getFriends(Integer userId) {
        String sql = """
                SELECT u.id, u.email, u.login, u.name, u.birthday
                FROM users u
                JOIN friendships f ON f.friend_id = u.id
                WHERE f.user_id = ?
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, rowMapper, userId);
    }

    @Override
    public List<User> getCommonFriends(Integer userId, Integer otherId) {
        String sql = """
                SELECT u.id, u.email, u.login, u.name, u.birthday
                FROM users u
                JOIN friendships first_friend ON first_friend.friend_id = u.id
                JOIN friendships second_friend ON second_friend.friend_id = u.id
                WHERE first_friend.user_id = ? AND second_friend.user_id = ?
                ORDER BY u.id
                """;
        return jdbcTemplate.query(sql, rowMapper, userId, otherId);
    }
}
