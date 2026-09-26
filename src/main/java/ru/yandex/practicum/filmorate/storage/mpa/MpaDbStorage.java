package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage implements MpaStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper rowMapper = new MpaRowMapper();

    @Override
    public List<Mpa> findAll() {
        return jdbcTemplate.query("SELECT id, name FROM mpa ORDER BY id", rowMapper);
    }

    @Override
    public Optional<Mpa> findById(Integer id) {
        return jdbcTemplate.query("SELECT id, name FROM mpa WHERE id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }
}
