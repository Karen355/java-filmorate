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
}
