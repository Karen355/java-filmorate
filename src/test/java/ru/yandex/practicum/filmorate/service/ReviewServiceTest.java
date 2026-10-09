package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService")
class ReviewServiceTest {

    @Mock
    private ReviewStorage reviewStorage;
    @Mock
    private UserStorage userStorage;
    @Mock
    private FilmStorage filmStorage;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("create: несуществующий пользователь - NotFoundException, отзыв не сохраняется")
    void create_unknownUser_throws() {
        when(userStorage.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(review(99, 1)))
                .isInstanceOf(NotFoundException.class);
        verify(reviewStorage, never()).create(any());
    }

    @Test
    @DisplayName("create: несуществующий фильм - NotFoundException, отзыв не сохраняется")
    void create_unknownFilm_throws() {
        when(userStorage.findById(1)).thenReturn(Optional.of(new User()));
        when(filmStorage.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(review(1, 99)))
                .isInstanceOf(NotFoundException.class);
        verify(reviewStorage, never()).create(any());
    }

    @Test
    @DisplayName("update: возвращает отзыв из хранилища, а не из запроса")
    void update_returnsStoredReview() {
        Review request = review(2, 2);
        request.setReviewId(5);
        Review stored = review(1, 1);
        stored.setReviewId(5);
        when(reviewStorage.findById(5)).thenReturn(Optional.of(stored));

        assertThat(reviewService.update(request)).isSameAs(stored);
        verify(reviewStorage).update(request);
    }

    @Test
    @DisplayName("findAll: отрицательный count - ValidationException, count = 0 - пустой список")
    void findAll_validatesCount() {
        assertThatThrownBy(() -> reviewService.findAll(null, -1))
                .isInstanceOf(ValidationException.class);
        assertThat(reviewService.findAll(null, 0)).isEmpty();
        verify(reviewStorage, never()).findAll(any(), anyInt());
    }

    @Test
    @DisplayName("findAll: несуществующий фильм - NotFoundException")
    void findAll_unknownFilm_throws() {
        when(filmStorage.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.findAll(99, 10))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("findAll: фильтр по фильму передаётся в хранилище")
    void findAll_passesFilter() {
        when(filmStorage.findById(1)).thenReturn(Optional.of(new Film()));
        when(reviewStorage.findAll(1, 10)).thenReturn(List.of(review(1, 1)));

        assertThat(reviewService.findAll(1, 10)).hasSize(1);
    }

    @Test
    @DisplayName("addLike / addDislike: несуществующий отзыв - NotFoundException")
    void reactions_unknownReview_throws() {
        when(reviewStorage.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addLike(99, 1)).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> reviewService.addDislike(99, 1)).isInstanceOf(NotFoundException.class);
        verify(reviewStorage, never()).addReaction(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    @DisplayName("лайки и дизлайки передаются в хранилище с правильным типом оценки")
    void reactions_passTypeToStorage() {
        when(reviewStorage.findById(1)).thenReturn(Optional.of(review(1, 1)));
        when(userStorage.findById(2)).thenReturn(Optional.of(new User()));

        reviewService.addLike(1, 2);
        reviewService.addDislike(1, 2);
        reviewService.removeLike(1, 2);
        reviewService.removeDislike(1, 2);

        verify(reviewStorage).addReaction(1, 2, true);
        verify(reviewStorage).addReaction(1, 2, false);
        verify(reviewStorage).removeReaction(1, 2, true);
        verify(reviewStorage).removeReaction(1, 2, false);
    }

    private Review review(Integer userId, Integer filmId) {
        return Review.builder()
                .content("Review")
                .isPositive(true)
                .userId(userId)
                .filmId(filmId)
                .build();
    }
}
