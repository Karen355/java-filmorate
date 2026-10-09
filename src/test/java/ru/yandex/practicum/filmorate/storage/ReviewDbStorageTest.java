package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.review.ReviewDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({ReviewDbStorage.class, FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("ReviewDbStorage")
class ReviewDbStorageTest {

    private final ReviewDbStorage reviewStorage;
    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    private Film film;
    private Film otherFilm;
    private User author;
    private User firstUser;
    private User secondUser;

    @BeforeEach
    void setUp() {
        film = filmStorage.create(film("Film"));
        otherFilm = filmStorage.create(film("Other"));
        author = userStorage.create(user("author"));
        firstUser = userStorage.create(user("first"));
        secondUser = userStorage.create(user("second"));
    }

    @Test
    @DisplayName("create и findById: поля сохраняются, рейтинг полезности равен нулю")
    void createAndFindById_persistsReview() {
        Review created = reviewStorage.create(review(film, true));

        assertThat(created.getReviewId()).isPositive();
        assertThat(reviewStorage.findById(created.getReviewId())).contains(created);
        assertThat(created.getUseful()).isZero();
        assertThat(reviewStorage.findById(-1)).isEmpty();
    }

    @Test
    @DisplayName("update: меняет текст и тип, но не автора и фильм")
    void update_changesOnlyContentAndType() {
        Review created = reviewStorage.create(review(film, true));
        Review changes = Review.builder()
                .reviewId(created.getReviewId())
                .content("Changed")
                .isPositive(false)
                .userId(firstUser.getId())
                .filmId(otherFilm.getId())
                .build();

        reviewStorage.update(changes);

        Review stored = reviewStorage.findById(created.getReviewId()).orElseThrow();
        assertThat(stored.getContent()).isEqualTo("Changed");
        assertThat(stored.getIsPositive()).isFalse();
        assertThat(stored.getUserId()).isEqualTo(author.getId());
        assertThat(stored.getFilmId()).isEqualTo(film.getId());
        changes.setReviewId(-1);
        assertThatThrownBy(() -> reviewStorage.update(changes)).isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("реакции: лайк +1, дизлайк -1, повторная оценка заменяет прежнюю")
    void reactions_changeUsefulness() {
        Review review = reviewStorage.create(review(film, true));
        Integer id = review.getReviewId();

        reviewStorage.addReaction(id, firstUser.getId(), true);
        reviewStorage.addReaction(id, firstUser.getId(), true);
        reviewStorage.addReaction(id, secondUser.getId(), true);
        assertThat(useful(id)).isEqualTo(2);

        reviewStorage.addReaction(id, secondUser.getId(), false);
        assertThat(useful(id)).isZero();

        reviewStorage.removeReaction(id, secondUser.getId(), true);
        assertThat(useful(id)).isZero();

        reviewStorage.removeReaction(id, secondUser.getId(), false);
        reviewStorage.removeReaction(id, firstUser.getId(), true);
        assertThat(useful(id)).isZero();

        reviewStorage.addReaction(id, firstUser.getId(), false);
        assertThat(useful(id)).isEqualTo(-1);
    }

    @Test
    @DisplayName("findAll: сортировка по полезности, затем по id; фильтр по фильму и лимит")
    void findAll_sortsFiltersAndLimits() {
        Review plain = reviewStorage.create(review(film, true));
        Review liked = reviewStorage.create(review(film, false));
        Review disliked = reviewStorage.create(review(film, true));
        Review other = reviewStorage.create(review(otherFilm, true));
        reviewStorage.addReaction(liked.getReviewId(), firstUser.getId(), true);
        reviewStorage.addReaction(disliked.getReviewId(), firstUser.getId(), false);

        assertThat(reviewStorage.findAll(null, 10)).extracting(Review::getReviewId)
                .containsExactly(liked.getReviewId(), plain.getReviewId(), other.getReviewId(),
                        disliked.getReviewId());
        assertThat(reviewStorage.findAll(film.getId(), 10)).extracting(Review::getReviewId)
                .containsExactly(liked.getReviewId(), plain.getReviewId(), disliked.getReviewId());
        assertThat(reviewStorage.findAll(film.getId(), 1)).extracting(Review::getUseful)
                .containsExactly(1);
    }

    @Test
    @DisplayName("delete: удаляет отзыв вместе с оценками; удаление фильма удаляет его отзывы")
    void delete_cascades() {
        Review review = reviewStorage.create(review(film, true));
        Review otherReview = reviewStorage.create(review(otherFilm, true));
        reviewStorage.addReaction(review.getReviewId(), firstUser.getId(), true);

        reviewStorage.delete(review.getReviewId());
        filmStorage.delete(otherFilm.getId());

        assertThat(reviewStorage.findById(review.getReviewId())).isEmpty();
        assertThat(reviewStorage.findById(otherReview.getReviewId())).isEmpty();
        assertThat(reviewStorage.findAll(null, 10)).isEmpty();
    }

    private int useful(Integer reviewId) {
        return reviewStorage.findById(reviewId).orElseThrow().getUseful();
    }

    private Review review(Film target, boolean isPositive) {
        return Review.builder()
                .content("Review")
                .isPositive(isPositive)
                .userId(author.getId())
                .filmId(target.getId())
                .build();
    }

    private Film film(String name) {
        return Film.builder()
                .name(name)
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .mpa(new Mpa(1, null))
                .build();
    }

    private User user(String login) {
        return User.builder()
                .email(login + "@mail.ru")
                .login(login)
                .name(login)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
