package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис для работы с фильмами. Хранение в памяти.
 */
@Slf4j
@Service
public class FilmService {

    private final Map<Integer, Film> films = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    public Film create(Film film) {
        film.setId(idGenerator.getAndIncrement());
        films.put(film.getId(), film);
        log.info("Добавлен фильм: id={}, name={}", film.getId(), film.getName());
        return film;
    }

    public Film update(Film film) {
        if (film.getId() == null || !films.containsKey(film.getId())) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }
        films.put(film.getId(), film);
        log.info("Обновлён фильм: id={}, name={}", film.getId(), film.getName());
        return film;
    }

    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }
}
