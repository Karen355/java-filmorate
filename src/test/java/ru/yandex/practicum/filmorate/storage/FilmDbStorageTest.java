package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@DisplayName("FilmDbStorage")
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;

    @Test
    @DisplayName("create и findById: все поля, рейтинг и жанры сохраняются")
    void createAndFindById_persistsFilmWithReferences() {
        Film film = film("Film");
        film.setId(99999);
        film.setGenres(List.of(new Genre(2, null), new Genre(1, null), new Genre(2, "Дубль")));

        Film created = filmStorage.create(film);
        Film stored = filmStorage.findById(created.getId()).orElseThrow();

        assertThat(created.getId()).isPositive().isNotEqualTo(99999);
        assertThat(stored).usingRecursiveComparison().ignoringFields("genres", "mpa").isEqualTo(created);
        assertThat(stored.getMpa()).isEqualTo(new Mpa(1, "G"));
        assertThat(stored.getGenres()).containsExactly(new Genre(1, "Комедия"), new Genre(2, "Драма"));
    }

    @Test
    @DisplayName("findById: отсутствующий фильм возвращает Optional.empty")
    void findById_unknown_returnsEmpty() {
        assertThat(filmStorage.findById(-1)).isEmpty();
    }

    @Test
    @DisplayName("findAll: пустая база, фильмы с жанрами и без жанров")
    void findAll_returnsFilmsWithTheirGenres() {
        assertThat(filmStorage.findAll()).isEmpty();
        Film first = film("First");
        first.setGenres(List.of(new Genre(6, "Боевик")));
        filmStorage.create(first);
        Film second = filmStorage.create(film("Second"));

        List<Film> films = filmStorage.findAll();

        assertThat(films).extracting(Film::getId).containsExactly(first.getId(), second.getId());
        assertThat(films.getFirst().getGenres()).containsExactly(new Genre(6, "Боевик"));
        assertThat(films.getLast().getGenres()).isEmpty();
    }

    @Test
    @DisplayName("update: заменяет поля, рейтинг и жанры, сохраняет лайки")
    void update_replacesFieldsAndGenresAndPreservesLikes() {
        Film film = film("Original");
        film.setGenres(List.of(new Genre(1, null)));
        filmStorage.create(film);
        User user = userStorage.create(user("first"));
        filmStorage.addLike(film.getId(), user.getId());
        film.setName("Updated");
        film.setDescription("Updated description");
        film.setReleaseDate(LocalDate.of(2005, 2, 3));
        film.setDuration(150);
        film.setMpa(new Mpa(5, "NC-17"));
        film.setGenres(List.of(new Genre(3, "Мультфильм")));

        filmStorage.update(film);

        assertThat(filmStorage.findById(film.getId())).contains(film);
        assertThat(filmStorage.getLikeCount(film.getId())).isEqualTo(1);

        film.setGenres(List.of());
        filmStorage.update(film);

        assertThat(filmStorage.findById(film.getId()).orElseThrow().getGenres()).isEmpty();
    }

    @Test
    @DisplayName("update: отсутствующий фильм не создаётся")
    void update_unknown_throwsNotFound() {
        Film film = film("Unknown");
        film.setId(-1);

        assertThatThrownBy(() -> filmStorage.update(film)).isInstanceOf(NotFoundException.class);
        assertThat(filmStorage.findAll()).isEmpty();
    }

    @Test
    @DisplayName("addLike, removeLike и getLikeCount: повторные операции идемпотентны")
    void likes_areIdempotent() {
        Film film = filmStorage.create(film("Film"));
        User first = userStorage.create(user("first"));
        User second = userStorage.create(user("second"));
        assertThat(filmStorage.getLikeCount(film.getId())).isZero();

        filmStorage.addLike(film.getId(), first.getId());
        filmStorage.addLike(film.getId(), first.getId());
        filmStorage.addLike(film.getId(), second.getId());

        assertThat(filmStorage.getLikeCount(film.getId())).isEqualTo(2);

        filmStorage.removeLike(film.getId(), first.getId());
        filmStorage.removeLike(film.getId(), first.getId());

        assertThat(filmStorage.getLikeCount(film.getId())).isEqualTo(1);
        userStorage.delete(second.getId());
        assertThat(filmStorage.getLikeCount(film.getId())).isZero();
    }

    @Test
    @DisplayName("getPopular: лайки по убыванию, id при равенстве, лимит и жанры")
    void getPopular_sortsAndLimitsFilms() {
        Film first = filmStorage.create(film("First"));
        Film second = filmStorage.create(film("Second"));
        Film third = film("Third");
        third.setGenres(List.of(new Genre(1, null), new Genre(2, null)));
        filmStorage.create(third);
        Film fourth = filmStorage.create(film("Fourth"));
        User firstUser = userStorage.create(user("first"));
        User secondUser = userStorage.create(user("second"));
        filmStorage.addLike(second.getId(), firstUser.getId());
        filmStorage.addLike(third.getId(), firstUser.getId());
        filmStorage.addLike(third.getId(), secondUser.getId());
        filmStorage.addLike(fourth.getId(), firstUser.getId());

        assertThat(filmStorage.getPopular(2)).extracting(Film::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(filmStorage.getPopular(10)).extracting(Film::getId)
                .containsExactly(third.getId(), second.getId(), fourth.getId(), first.getId());
        assertThat(filmStorage.getPopular(1).getFirst().getGenres())
                .containsExactly(new Genre(1, "Комедия"), new Genre(2, "Драма"));
        assertThat(filmStorage.getPopular(0)).isEmpty();
    }

    @Test
    @DisplayName("delete: удаляет фильм, жанры и лайки, сохраняет пользователя")
    void delete_cascadesToGenresAndLikes() {
        Film film = film("Film");
        film.setGenres(List.of(new Genre(1, null)));
        filmStorage.create(film);
        User user = userStorage.create(user("first"));
        filmStorage.addLike(film.getId(), user.getId());

        filmStorage.delete(film.getId());
        filmStorage.delete(film.getId());

        assertThat(filmStorage.findById(film.getId())).isEmpty();
        assertThat(filmStorage.findAll()).isEmpty();
        assertThat(filmStorage.getLikeCount(film.getId())).isZero();
        assertThat(userStorage.findById(user.getId())).contains(user);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("create: ошибка жанра откатывает вставку фильма")
    void create_invalidGenre_rollsBackFilm() {
        Film film = film("Invalid");
        film.setGenres(List.of(new Genre(1, null), new Genre(999, null)));

        assertThatThrownBy(() -> filmStorage.create(film)).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(filmStorage.findAll()).isEmpty();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("update: ошибка жанра откатывает поля фильма и замену жанров")
    void update_invalidGenre_rollsBackAllChanges() {
        Film film = film("Original");
        film.setGenres(List.of(new Genre(1, null)));
        filmStorage.create(film);
        try {
            Film original = filmStorage.findById(film.getId()).orElseThrow();
            film.setName("Invalid update");
            film.setGenres(List.of(new Genre(2, null), new Genre(999, null)));

            assertThatThrownBy(() -> filmStorage.update(film)).isInstanceOf(DataIntegrityViolationException.class);

            assertThat(filmStorage.findById(film.getId())).contains(original);
        } finally {
            filmStorage.delete(film.getId());
        }
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
