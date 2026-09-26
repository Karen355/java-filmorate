package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.GenreService;
import ru.yandex.practicum.filmorate.service.MpaService;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Сохранение данных после перезапуска приложения")
class DatabasePersistenceTest {

    @TempDir
    private Path databaseDirectory;

    @Test
    @DisplayName("Фильмы, пользователи, жанры, рейтинги, лайки и дружба сохраняются между запусками")
    void restart_preservesDataAndDoesNotDuplicateReferences() {
        User first;
        User second;
        Film film;
        try (ConfigurableApplicationContext context = startApplication()) {
            UserService userService = context.getBean(UserService.class);
            FilmService filmService = context.getBean(FilmService.class);
            first = userService.create(user("first"));
            second = userService.create(user("second"));
            userService.addFriend(first.getId(), second.getId());
            film = filmService.create(Film.builder()
                    .name("Persistent film")
                    .description("Description")
                    .releaseDate(LocalDate.of(2000, 1, 1))
                    .duration(100)
                    .mpa(new Mpa(3, null))
                    .genres(List.of(new Genre(2, null), new Genre(1, null)))
                    .build());
            filmService.addLike(film.getId(), first.getId());
        }

        try (ConfigurableApplicationContext context = startApplication()) {
            UserService userService = context.getBean(UserService.class);
            FilmService filmService = context.getBean(FilmService.class);
            assertThat(userService.findAll()).containsExactly(first, second);
            assertThat(userService.getFriends(first.getId())).containsExactly(second);
            assertThat(userService.getFriends(second.getId())).isEmpty();
            assertThat(filmService.findById(film.getId())).isEqualTo(film);
            assertThat(filmService.getPopular(10)).containsExactly(film);
            assertThat(context.getBean(FilmDbStorage.class).getLikeCount(film.getId())).isEqualTo(1);
            assertThat(context.getBean(MpaService.class).findAll()).hasSize(5);
            assertThat(context.getBean(GenreService.class).findAll()).hasSize(6);
            assertThat(userService.create(user("third")).getId()).isGreaterThan(second.getId());
        }
    }

    private ConfigurableApplicationContext startApplication() {
        String databasePath = databaseDirectory.resolve("filmorate").toAbsolutePath().toString().replace('\\', '/');
        return new SpringApplicationBuilder(FilmorateApplication.class)
                .web(WebApplicationType.NONE)
                .run("--spring.datasource.url=jdbc:h2:file:" + databasePath, "--spring.main.banner-mode=off");
    }

    private User user(String login) {
        return User.builder()
                .email(login + "@mail.ru")
                .login(login)
                .birthday(LocalDate.of(1990, 1, 1))
                .build();
    }
}
