package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Бизнес-логика для фильмов: CRUD, лайки, популярные фильмы.
 */
@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final GenreService genreService;
    private final MpaService mpaService;
    private final DirectorService directorService;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage,
                       GenreService genreService, MpaService mpaService, DirectorService directorService) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreService = genreService;
        this.mpaService = mpaService;
        this.directorService = directorService;
    }

    public Film create(Film film) {
        normalizeReferences(film);
        Film created = filmStorage.create(film);
        log.info("Добавлен фильм: id={}, name={}", created.getId(), created.getName());
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null || filmStorage.findById(film.getId()).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        normalizeReferences(film);
        filmStorage.update(film);
        log.info("Обновлён фильм: id={}, name={}", film.getId(), film.getName());
        return film;
    }

    public void delete(Integer id) {
        ensureFilmExists(id);
        filmStorage.delete(id);
        log.info("Удалён фильм: id={}", id);
    }

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Integer id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id=" + id + " не найден"));
    }

    public void addLike(Integer filmId, Integer userId) {
        ensureFilmExists(filmId);
        ensureUserExists(userId);
        filmStorage.addLike(filmId, userId);
        log.info("Пользователь id={} поставил лайк фильму id={}", userId, filmId);
    }

    public void removeLike(Integer filmId, Integer userId) {
        ensureFilmExists(filmId);
        ensureUserExists(userId);
        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь id={} убрал лайк с фильма id={}", userId, filmId);
    }

    /**
     * Популярные фильмы по числу лайков. Если count меньше нуля - ошибка валидации.
     * Если count равен нулю - пустой список. Если параметр не передан - контроллер подставляет 10.
     */
    public List<Film> getPopular(int count) {
        if (count < 0) {
            throw new ValidationException("Параметр count не может быть отрицательным");
        }
        if (count == 0) {
            return List.of();
        }
        return filmStorage.getPopular(count);
    }

    public List<Film> getCommonFilms(Integer userId, Integer friendId) {
    ensureUserExists(userId);
    ensureUserExists(friendId);
    return filmStorage.getCommonFilms(userId, friendId);
}

public List<Film> findByDirector(Integer directorId, String sortBy) {
    if (!"year".equals(sortBy) && !"likes".equals(sortBy)) {
        throw new ValidationException("Параметр sortBy должен быть year или likes");
    }

    directorService.findById(directorId);
    return filmStorage.findByDirector(directorId, sortBy);
}

    public List<Film> search(String query, String by) {
        Set<String> fields = Arrays.stream(by.split(",", -1))
                .map(String::trim)
                .collect(Collectors.toSet());
        if (fields.stream().anyMatch(field -> !field.equals("title") && !field.equals("director"))) {
            throw new ValidationException("Параметр by должен содержать title или director");
        }
        String searchQuery = query.strip();
        if (searchQuery.isEmpty()) {
            return List.of();
        }
        return filmStorage.search(searchQuery, fields.contains("title"), fields.contains("director"));
    }

    private void ensureFilmExists(Integer filmId) {
        if (filmStorage.findById(filmId).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
    }

    private void normalizeReferences(Film film) {
        if (film.getMpa() == null || film.getMpa().getId() == null) {
            throw new ValidationException("Рейтинг MPA обязателен");
        }
        film.setMpa(mpaService.findById(film.getMpa().getId()));
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            film.setGenres(List.of());
        } else {
            Map<Integer, Genre> availableGenres = genreService.findAll().stream()
                    .collect(Collectors.toMap(Genre::getId, Function.identity()));
            Map<Integer, Genre> genresById = new TreeMap<>();
            for (Genre genre : film.getGenres()) {
                if (genre == null || genre.getId() == null) {
                    throw new ValidationException("Идентификатор жанра обязателен");
                }
                Genre storedGenre = availableGenres.get(genre.getId());
                if (storedGenre == null) {
                    throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                }
                genresById.put(storedGenre.getId(), storedGenre);
            }
            film.setGenres(List.copyOf(genresById.values()));
        }
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            film.setDirectors(List.of());
            return;
        }
        Map<Integer, Director> directorsById = new TreeMap<>();
        for (Director director : film.getDirectors()) {
            if (director == null || director.getId() == null) {
                throw new ValidationException("Идентификатор режиссёра обязателен");
            }
            Director storedDirector = directorService.findById(director.getId());
            directorsById.put(storedDirector.getId(), storedDirector);
        }
        film.setDirectors(List.copyOf(directorsById.values()));
    }

    private void ensureUserExists(Integer userId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
