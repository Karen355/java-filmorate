package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

/**
 * Хранилище отзывов и оценок отзывов.
 */
public interface ReviewStorage {

    Review create(Review review);

    /**
     * Обновляет текст и тип отзыва. Автор и фильм не меняются.
     */
    void update(Review review);

    void delete(Integer id);

    Optional<Review> findById(Integer id);

    /**
     * Отзывы по убыванию полезности, при равенстве - по id.
     *
     * @param filmId фильм для фильтрации, {@code null} - отзывы на все фильмы
     */
    List<Review> findAll(Integer filmId, int count);

    /**
     * Ставит оценку отзыву, заменяя предыдущую оценку этого пользователя.
     *
     * @param isUseful {@code true} - лайк, {@code false} - дизлайк
     */
    void addReaction(Integer reviewId, Integer userId, boolean isUseful);

    /**
     * Удаляет оценку пользователя, если она совпадает с указанной.
     */
    void removeReaction(Integer reviewId, Integer userId, boolean isUseful);
}
