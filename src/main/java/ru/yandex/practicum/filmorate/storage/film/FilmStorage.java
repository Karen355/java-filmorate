package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

/**
 * Хранилище фильмов.
 */
public interface FilmStorage {

    Film create(Film film);

    void update(Film film);

    void delete(Integer id);

    Optional<Film> findById(Integer id);

    List<Film> findAll();

    void addLike(Integer filmId, Integer userId);

    void removeLike(Integer filmId, Integer userId);

    long getLikeCount(Integer filmId);

    /**
     * Самые популярные фильмы по числу лайков.
     *
     * @param genreId жанр для фильтрации, {@code null} - без фильтра по жанру
     * @param year    год релиза для фильтрации, {@code null} - без фильтра по году
     */
    List<Film> getPopular(int count, Integer genreId, Integer year);

    List<Film> findByDirector(Integer directorId, String sortBy);

    List<Film> search(String query, boolean byTitle, boolean byDirector);
}
