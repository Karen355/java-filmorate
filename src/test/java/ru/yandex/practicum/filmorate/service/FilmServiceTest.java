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
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.feed.FeedStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    @Mock
    private FeedStorage feedStorage;
    @Mock
    private GenreService genreService;
    @Mock
    private MpaService mpaService;
    @Mock
    private DirectorService directorService;

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
        when(filmStorage.getPopular(10, null, null)).thenReturn(List.of(film2, film1));

        List<Film> popular = filmService.getPopular(10, null, null);

        assertThat(popular).extracting(Film::getId).containsExactly(2, 1);
        verify(filmStorage).getPopular(10, null, null);
    }

    @Test
    @DisplayName("getPopular: отрицательный count - ValidationException")
    void getPopular_negativeCount_throws() {
        assertThatThrownBy(() -> filmService.getPopular(-1, null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("getPopular: count = 0 - пустой список, storage не вызывается")
    void getPopular_zeroCount_returnsEmpty() {
        List<Film> popular = filmService.getPopular(0, null, null);
        assertThat(popular).isEmpty();
        verify(filmStorage, never()).getPopular(anyInt(), any(), any());
    }

    @Test
    @DisplayName("getPopular: фильтры по жанру и году передаются в storage")
    void getPopular_withFilters_passesThemToStorage() {
        when(filmStorage.getPopular(5, 1, 2000)).thenReturn(List.of(film1));

        List<Film> popular = filmService.getPopular(5, 1, 2000);

        assertThat(popular).containsExactly(film1);
        verify(genreService).findById(1);
    }

    @Test
    @DisplayName("getPopular: несуществующий жанр - NotFoundException, storage не вызывается")
    void getPopular_unknownGenre_throws() {
        when(genreService.findById(999)).thenThrow(new NotFoundException("Жанр с id=999 не найден"));

        assertThatThrownBy(() -> filmService.getPopular(10, 999, null))
                .isInstanceOf(NotFoundException.class);
        verify(filmStorage, never()).getPopular(anyInt(), any(), any());
    }

    @Test
    @DisplayName("getCommonFilms: проверяет пользователей и возвращает результат хранилища")
    void getCommonFilms_returnsFilmsFromStorage() {
        when(filmStorage.getCommonFilms(1, 2)).thenReturn(List.of(film2, film1));

        List<Film> commonFilms = filmService.getCommonFilms(1, 2);

        assertThat(commonFilms).extracting(Film::getId).containsExactly(2, 1);
        verify(userStorage).findById(1);
        verify(userStorage).findById(2);
        verify(filmStorage).getCommonFilms(1, 2);
    }

    @Test
    @DisplayName("getCommonFilms: неизвестный пользователь - NotFoundException")
    void getCommonFilms_unknownUser_throws() {
        when(userStorage.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filmService.getCommonFilms(99, 1))
                .isInstanceOf(NotFoundException.class);
        verify(filmStorage, never()).getCommonFilms(anyInt(), anyInt());
    }

    @Test
    @DisplayName("addLike: несуществующий фильм - NotFoundException")
    void addLike_filmNotFound_throws() {
        when(filmStorage.findById(99)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> filmService.addLike(99, 1))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("addLike: записывает событие в ленту")
    void addLike_recordsFeedEvent() {
        when(filmStorage.findById(1)).thenReturn(Optional.of(film1));
        when(filmStorage.addLike(1, 1)).thenReturn(true);

        filmService.addLike(1, 1);

        verify(filmStorage).addLike(1, 1);
        verify(feedStorage).addEvent(1, EventType.LIKE, Operation.ADD, 1);
    }

    @Test
    @DisplayName("addLike: повторный лайк не записывает событие в ленту")
    void addLike_withoutStateChange_doesNotRecordFeedEvent() {
        when(filmStorage.findById(1)).thenReturn(Optional.of(film1));
        when(filmStorage.addLike(1, 1)).thenReturn(false);

        filmService.addLike(1, 1);

        verify(filmStorage).addLike(1, 1);
        verify(feedStorage, never()).addEvent(1, EventType.LIKE, Operation.ADD, 1);
    }

    @Test
    @DisplayName("removeLike: снятие отсутствующего лайка не записывает событие в ленту")
    void removeLike_withoutStateChange_doesNotRecordFeedEvent() {
        when(filmStorage.findById(1)).thenReturn(Optional.of(film1));
        when(filmStorage.removeLike(1, 1)).thenReturn(false);

        filmService.removeLike(1, 1);

        verify(filmStorage).removeLike(1, 1);
        verify(feedStorage, never()).addEvent(1, EventType.LIKE, Operation.REMOVE, 1);
    }
}
