package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.sql.PreparedStatement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private static final String SELECT_FILMS = """
            SELECT f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name AS mpa_name
            FROM films f
            JOIN mpa m ON m.id = f.mpa_id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper rowMapper = new FilmRowMapper();

    @Override
    @Transactional
    public Film create(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setObject(3, film.getReleaseDate());
            statement.setInt(4, film.getDuration());
            statement.setInt(5, film.getMpa().getId());
            return statement;
        }, keyHolder);
        film.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        saveGenres(film);
        saveDirectors(film);
        return film;
    }

    @Override
    @Transactional
    public void update(Film film) {
        String sql = """
                UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
                WHERE id = ?
                """;
        int updated = jdbcTemplate.update(sql, film.getName(), film.getDescription(), film.getReleaseDate(),
                film.getDuration(), film.getMpa().getId(), film.getId());
        if (updated == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        saveGenres(film);
        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        saveDirectors(film);
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM films WHERE id = ?", id);
    }

    @Override
    public Optional<Film> findById(Integer id) {
        List<Film> films = jdbcTemplate.query(SELECT_FILMS + "WHERE f.id = ?", rowMapper, id);
        loadGenres(films);
        loadDirectors(films);
        return films.stream().findFirst();
    }

    @Override
    public List<Film> findAll() {
        List<Film> films = jdbcTemplate.query(SELECT_FILMS + "ORDER BY f.id", rowMapper);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public boolean addLike(Integer filmId, Integer userId) {
        int updated = jdbcTemplate.update("""
                INSERT INTO film_likes (film_id, user_id)
                SELECT ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM film_likes WHERE film_id = ? AND user_id = ?
                )
                """, filmId, userId, filmId, userId);
        return updated > 0;
    }

    @Override
    public boolean removeLike(Integer filmId, Integer userId) {
        return jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId) > 0;
    }

    @Override
    public long getLikeCount(Integer filmId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM film_likes WHERE film_id = ?", Long.class, filmId));
    }

    @Override
    public List<Film> getPopular(int count) {
        String sql = SELECT_FILMS + """
                LEFT JOIN (SELECT film_id, COUNT(*) AS like_count FROM film_likes GROUP BY film_id) likes
                    ON likes.film_id = f.id
                ORDER BY COALESCE(likes.like_count, 0) DESC, f.id
                LIMIT ?
                """;
        List<Film> films = jdbcTemplate.query(sql, rowMapper, count);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public List<Film> findByDirector(Integer directorId, String sortBy) {
        String orderBy = "year".equals(sortBy)
                ? "ORDER BY f.release_date, f.id"
                : "ORDER BY COALESCE(likes.like_count, 0) DESC, f.id";
        String sql = SELECT_FILMS + """
                JOIN film_directors fd ON fd.film_id = f.id
                LEFT JOIN (SELECT film_id, COUNT(*) AS like_count FROM film_likes GROUP BY film_id) likes
                    ON likes.film_id = f.id
                WHERE fd.director_id = ?
                """ + orderBy;
        List<Film> films = jdbcTemplate.query(sql, rowMapper, directorId);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public List<Film> search(String query, boolean byTitle, boolean byDirector) {
        String sql = SELECT_FILMS + """
                LEFT JOIN (SELECT film_id, COUNT(*) AS like_count FROM film_likes GROUP BY film_id) likes
                    ON likes.film_id = f.id
                WHERE (? = TRUE AND LOCATE(LOWER(?), LOWER(f.name)) > 0)
                    OR (? = TRUE AND EXISTS (
                        SELECT 1 FROM film_directors fd
                        JOIN directors d ON d.id = fd.director_id
                        WHERE fd.film_id = f.id AND LOCATE(LOWER(?), LOWER(d.name)) > 0
                    ))
                ORDER BY COALESCE(likes.like_count, 0) DESC, f.id
                """;
        List<Film> films = jdbcTemplate.query(sql, rowMapper, byTitle, query, byDirector, query);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    private void saveGenres(Film film) {
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        List<Object[]> parameters = film.getGenres().stream()
                .map(Genre::getId)
                .distinct()
                .map(id -> new Object[]{film.getId(), id})
                .toList();
        jdbcTemplate.batchUpdate("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)", parameters);
    }

    private void loadGenres(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Integer, Film> filmsById = films.stream().collect(Collectors.toMap(Film::getId, Function.identity()));
        String placeholders = String.join(", ", Collections.nCopies(films.size(), "?"));
        String sql = """
                SELECT fg.film_id, g.id, g.name
                FROM film_genres fg
                JOIN genres g ON g.id = fg.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY g.id
                """.formatted(placeholders);
        jdbcTemplate.query(sql, resultSet -> {
            Film film = filmsById.get(resultSet.getInt("film_id"));
            film.getGenres().add(new Genre(resultSet.getInt("id"), resultSet.getString("name")));
        }, filmsById.keySet().toArray());
    }

    private void saveDirectors(Film film) {
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }
        List<Object[]> parameters = film.getDirectors().stream()
                .map(Director::getId)
                .distinct()
                .map(id -> new Object[]{film.getId(), id})
                .toList();
        jdbcTemplate.batchUpdate("INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)", parameters);
    }

    private void loadDirectors(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Integer, Film> filmsById = films.stream().collect(Collectors.toMap(Film::getId, Function.identity()));
        String placeholders = String.join(", ", Collections.nCopies(films.size(), "?"));
        String sql = """
                SELECT fd.film_id, d.id, d.name
                FROM film_directors fd
                JOIN directors d ON d.id = fd.director_id
                WHERE fd.film_id IN (%s)
                ORDER BY d.id
                """.formatted(placeholders);
        jdbcTemplate.query(sql, resultSet -> {
            Film film = filmsById.get(resultSet.getInt("film_id"));
            film.getDirectors().add(new Director(resultSet.getInt("id"), resultSet.getString("name")));
        }, filmsById.keySet().toArray());
    }
}
