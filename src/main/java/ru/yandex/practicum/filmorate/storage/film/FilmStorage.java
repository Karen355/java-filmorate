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

    boolean addLike(Integer filmId, Integer userId);

    boolean removeLike(Integer filmId, Integer userId);

    long getLikeCount(Integer filmId);

    List<Film> getPopular(int count);

    List<Film> findByDirector(Integer directorId, String sortBy);

    List<Film> search(String query, boolean byTitle, boolean byDirector);
}
