package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

/**
 * Бизнес-логика для отзывов: CRUD, лайки и дизлайки отзывов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final FeedStorage feedStorage;

    public Review create(Review review) {
        ensureUserExists(review.getUserId());
        ensureFilmExists(review.getFilmId());
        Review created = reviewStorage.create(review);
        feedStorage.addEvent(created.getUserId(), EventType.REVIEW, Operation.ADD, created.getReviewId());
        log.info("Добавлен отзыв: id={}, userId={}, filmId={}",
                created.getReviewId(), created.getUserId(), created.getFilmId());
        return created;
    }

    /**
     * Меняет текст и тип отзыва. Автор и фильм отзыва остаются прежними.
     */
    public Review update(Review review) {
        if (review.getReviewId() == null) {
            throw new NotFoundException("Отзыв с id=null не найден");
        }
        reviewStorage.update(review);
        Review updated = findById(review.getReviewId());
        feedStorage.addEvent(updated.getUserId(), EventType.REVIEW, Operation.UPDATE, updated.getReviewId());
        log.info("Обновлён отзыв: id={}", review.getReviewId());
        return updated;
    }

    public void delete(Integer id) {
        Review review = findById(id);
        reviewStorage.delete(id);
        feedStorage.addEvent(review.getUserId(), EventType.REVIEW, Operation.REMOVE, id);
        log.info("Удалён отзыв: id={}", id);
    }

    public Review findById(Integer id) {
        return reviewStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Отзыв с id=" + id + " не найден"));
    }

    /**
     * Отзывы по убыванию полезности. Если filmId не указан - отзывы на все фильмы.
     */
    public List<Review> findAll(Integer filmId, int count) {
        if (count < 0) {
            throw new ValidationException("Параметр count не может быть отрицательным");
        }
        if (filmId != null) {
            ensureFilmExists(filmId);
        }
        if (count == 0) {
            return List.of();
        }
        return reviewStorage.findAll(filmId, count);
    }

    public void addLike(Integer reviewId, Integer userId) {
        addReaction(reviewId, userId, true);
    }

    public void addDislike(Integer reviewId, Integer userId) {
        addReaction(reviewId, userId, false);
    }

    public void removeLike(Integer reviewId, Integer userId) {
        removeReaction(reviewId, userId, true);
    }

    public void removeDislike(Integer reviewId, Integer userId) {
        removeReaction(reviewId, userId, false);
    }

    private void addReaction(Integer reviewId, Integer userId, boolean isUseful) {
        ensureReviewExists(reviewId);
        ensureUserExists(userId);
        reviewStorage.addReaction(reviewId, userId, isUseful);
        log.info("Пользователь id={} оценил отзыв id={}: {}", userId, reviewId, isUseful ? "лайк" : "дизлайк");
    }

    private void removeReaction(Integer reviewId, Integer userId, boolean isUseful) {
        ensureReviewExists(reviewId);
        ensureUserExists(userId);
        reviewStorage.removeReaction(reviewId, userId, isUseful);
        log.info("Пользователь id={} убрал {} с отзыва id={}", userId, isUseful ? "лайк" : "дизлайк", reviewId);
    }

    private void ensureReviewExists(Integer reviewId) {
        if (reviewStorage.findById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id=" + reviewId + " не найден");
        }
    }

    private void ensureUserExists(Integer userId) {
        if (userStorage.findById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void ensureFilmExists(Integer filmId) {
        if (filmStorage.findById(filmId).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + filmId + " не найден");
        }
    }
}
