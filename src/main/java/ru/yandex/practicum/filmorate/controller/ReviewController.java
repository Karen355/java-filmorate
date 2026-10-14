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
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.service.ReviewService;

import java.util.List;

/**
 * REST-контроллер для работы с отзывами.
 */
@Slf4j
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Review create(@Valid @RequestBody Review review) {
        log.debug("Запрос на добавление отзыва: userId={}, filmId={}", review.getUserId(), review.getFilmId());
        return reviewService.create(review);
    }

    @PutMapping
    public Review update(@Valid @RequestBody Review review) {
        log.debug("Запрос на обновление отзыва: id={}", review.getReviewId());
        return reviewService.update(review);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        log.debug("Запрос на удаление отзыва: id={}", id);
        reviewService.delete(id);
    }

    @GetMapping("/{id}")
    public Review findById(@PathVariable Integer id) {
        log.debug("Запрос на получение отзыва: id={}", id);
        return reviewService.findById(id);
    }

    @GetMapping
    public List<Review> findAll(
            @RequestParam(value = "filmId", required = false) Integer filmId,
            @RequestParam(value = "count", defaultValue = "10") int count) {
        log.debug("Запрос на список отзывов: filmId={}, count={}", filmId, count);
        return reviewService.findAll(filmId, count);
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.debug("Запрос на лайк отзыву: reviewId={}, userId={}", id, userId);
        reviewService.addLike(id, userId);
    }

    @PutMapping("/{id}/dislike/{userId}")
    public void addDislike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.debug("Запрос на дизлайк отзыву: reviewId={}, userId={}", id, userId);
        reviewService.addDislike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.debug("Запрос на снятие лайка с отзыва: reviewId={}, userId={}", id, userId);
        reviewService.removeLike(id, userId);
    }

    @DeleteMapping("/{id}/dislike/{userId}")
    public void removeDislike(@PathVariable Integer id, @PathVariable Integer userId) {
        log.debug("Запрос на снятие дизлайка с отзыва: reviewId={}, userId={}", id, userId);
        reviewService.removeDislike(id, userId);
    }
}
