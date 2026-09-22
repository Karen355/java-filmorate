package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FilmService")
class FilmServiceTest {

    @Mock
    private FilmStorage filmStorage;
    @Mock
    private UserStorage userStorage;

    @InjectMocks
    private FilmService filmService;

    private Film film1;
    private Film film2;

    @BeforeEach
    void setUp() {
        film1 = Film.builder()
                .id(1)
                .name("A")
                .description("d")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();
        film2 = Film.builder()
                .id(2)
                .name("B")
                .description("d")
                .releaseDate(LocalDate.of(2001, 1, 1))
                .duration(100)
                .build();
        lenient().when(userStorage.findById(anyInt())).thenReturn(Optional.of(
                ru.yandex.practicum.filmorate.model.User.builder()
                        .id(1).email("a@a.ru").login("a").birthday(LocalDate.of(1990, 1, 1)).build()));
    }

    @Test
    @DisplayName("getPopular: сортировка по числу лайков, затем по id")
    void getPopular_sortsByLikesThenId() {
        when(filmStorage.getPopular(10)).thenReturn(List.of(film2, film1));

        List<Film> popular = filmService.getPopular(10);

        assertThat(popular).extracting(Film::getId).containsExactly(2, 1);
        verify(filmStorage).getPopular(10);
    }

    @Test
    @DisplayName("getPopular: отрицательный count - ValidationException")
    void getPopular_negativeCount_throws() {
        assertThatThrownBy(() -> filmService.getPopular(-1))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("getPopular: count = 0 - пустой список, storage не вызывается")
    void getPopular_zeroCount_returnsEmpty() {
        List<Film> popular = filmService.getPopular(0);
        assertThat(popular).isEmpty();
        verify(filmStorage, never()).getPopular(anyInt());
    }

    @Test
    @DisplayName("addLike: несуществующий фильм - NotFoundException")
    void addLike_filmNotFound_throws() {
        when(filmStorage.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> filmService.addLike(99, 1))
                .isInstanceOf(NotFoundException.class);
    }
}
