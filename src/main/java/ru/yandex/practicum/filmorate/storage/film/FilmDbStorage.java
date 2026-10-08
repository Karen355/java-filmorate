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
import java.util.ArrayList;
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
    public void addLike(Integer filmId, Integer userId) {
        jdbcTemplate.update("MERGE INTO film_likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)",
                filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        jdbcTemplate.update("DELETE FROM film_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
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
    public List<Film> getRecommendations(Integer userId) {

        String similarUserSql = """
            SELECT other_likes.user_id,
                   COUNT(*) AS common_count
            FROM film_likes my_likes
            JOIN film_likes other_likes
                ON my_likes.film_id = other_likes.film_id
            WHERE my_likes.user_id = ?
                AND other_likes.user_id <> ?
            GROUP BY other_likes.user_id
            ORDER BY common_count DESC, other_likes.user_id
            """;

        List<Map.Entry<Integer, Long>> matches = jdbcTemplate.query(
                similarUserSql,
                (rs, rowNum) -> Map.entry(
                        rs.getInt("user_id"),
                        rs.getLong("common_count")
                ),
                userId, userId
        );

        if (matches.isEmpty()) {
            return Collections.emptyList();
        }

        long maxMatches = matches.get(0).getValue();

        List<Integer> similarUsers = matches.stream()
                .filter(match -> match.getValue() == maxMatches)
                .map(Map.Entry::getKey)
                .toList();

        String placeholders = String.join(
                ", ", Collections.nCopies(similarUsers.size(), "?")
        );

        String sql = SELECT_FILMS + """
            WHERE EXISTS (
                SELECT 1
                FROM film_likes peer_likes
                WHERE peer_likes.film_id = f.id
                    AND peer_likes.user_id IN (%s)
            )
            AND NOT EXISTS (
                SELECT 1
                FROM film_likes own_likes
                WHERE own_likes.film_id = f.id
                    AND own_likes.user_id = ?
            )
            ORDER BY f.id
            """.formatted(placeholders);

        List<Object> parameters = new ArrayList<>(similarUsers);
        parameters.add(userId);

        List<Film> films = jdbcTemplate.query(
                sql, rowMapper, parameters.toArray()
        );

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
