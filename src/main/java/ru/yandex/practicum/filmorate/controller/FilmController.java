package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.List;

/**
 * REST-контроллер для работы с фильмами.
 */
@Slf4j
@RestController
@RequestMapping("/films")
@RequiredArgsConstructor
public class FilmController {

    private final FilmService filmService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Film addFilm(@Valid @RequestBody Film film) {
        log.debug("Запрос на добавление фильма: {}", film.getName());
        return filmService.create(film);
    }

    @PutMapping
    public Film updateFilm(@Valid @RequestBody Film film) {
        log.debug("Запрос на обновление фильма: id={}", film.getId());
        return filmService.update(film);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(
            @RequestParam(value = "count", defaultValue = "10") int count) {
        log.debug("Запрос на популярные фильмы, count={}", count);
        return filmService.getPopular(count);
    }

    @GetMapping("/{id}")
    public Film getFilmById(@PathVariable Integer id) {
        log.debug("Запрос на получение фильма id={}", id);
        return filmService.findById(id);
    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.debug("Запрос на получение списка всех фильмов");
        return filmService.findAll();
    }

    @PutMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void addLike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.debug("Запрос на лайк: filmId={}, userId={}", id, userId);
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void removeLike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.debug("Запрос на снятие лайка: filmId={}, userId={}", id, userId);
        filmService.removeLike(id, userId);
    }
}
