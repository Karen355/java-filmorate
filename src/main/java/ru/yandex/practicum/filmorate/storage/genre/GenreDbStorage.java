package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage implements GenreStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper rowMapper = new GenreRowMapper();

    @Override
    public List<Genre> findAll() {
        return jdbcTemplate.query("SELECT id, name FROM genres ORDER BY id", rowMapper);
    }

    @Override
    public Optional<Genre> findById(Integer id) {
        return jdbcTemplate.query("SELECT id, name FROM genres WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }
}
