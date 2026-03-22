package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Бизнес-логика для фильмов: CRUD, лайки, популярные фильмы.
 */
@Slf4j
@Service
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    @Autowired
    public FilmService(FilmStorage filmStorage, UserStorage userStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
    }

    public Film create(Film film) {
        Film created = filmStorage.create(film);
        log.info("Добавлен фильм: id={}, name={}", created.getId(), created.getName());
        return created;
    }

    public Film update(Film film) {
        if (film.getId() == null || filmStorage.findById(film.getId()).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        filmStorage.update(film);
        log.info("Обновлён фильм: id={}, name={}", film.getId(), film.getName());
        return film;
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
        List<Film> films = new ArrayList<>(filmStorage.findAll());
        films.sort((a, b) -> {
            long la = filmStorage.getLikeCount(a.getId());
            long lb = filmStorage.getLikeCount(b.getId());
            int cmp = Long.compare(lb, la);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(
                    a.getId() == null ? Integer.MAX_VALUE : a.getId(),
                    b.getId() == null ? Integer.MAX_VALUE : b.getId());
        });
        return films.stream().limit(count).toList();
    }

    private void ensureFilmExists(Integer filmId) {
        if (filmStorage.findById(filmId).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
    }

    private void ensureUserExists(Integer userId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }
}
