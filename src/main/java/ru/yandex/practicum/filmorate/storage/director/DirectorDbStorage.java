package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbcTemplate;
    private final DirectorRowMapper rowMapper = new DirectorRowMapper();

    @Override
    public Director create(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, director.getName());
            return statement;
        }, keyHolder);
        director.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return director;
    }

    @Override
    public void update(Director director) {
        int updated = jdbcTemplate.update("UPDATE directors SET name = ? WHERE id = ?",
                director.getName(), director.getId());
        if (updated == 0) {
            throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден");
        }
    }

    @Override
    public void delete(Integer id) {
        int deleted = jdbcTemplate.update("DELETE FROM directors WHERE id = ?", id);
        if (deleted == 0) {
            throw new NotFoundException("Режиссёр с id=" + id + " не найден");
        }
    }

    @Override
    public Optional<Director> findById(Integer id) {
        return jdbcTemplate.query("SELECT id, name FROM directors WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Director> findAll() {
        return jdbcTemplate.query("SELECT id, name FROM directors ORDER BY id", rowMapper);
    }
}
