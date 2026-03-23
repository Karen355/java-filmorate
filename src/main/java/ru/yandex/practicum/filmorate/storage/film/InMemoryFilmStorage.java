package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory реализация хранилища фильмов и лайков (id пользователей в Set).
 */
@Component
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Integer, Film> films = new ConcurrentHashMap<>();
    private final Map<Integer, Set<Integer>> likes = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);

    @Override
    public Film create(Film film) {
        film.setId(idGenerator.getAndIncrement());
        films.put(film.getId(), film);
        likes.putIfAbsent(film.getId(), ConcurrentHashMap.newKeySet());
        return film;
    }

    @Override
    public void update(Film film) {
        films.put(film.getId(), film);
        likes.putIfAbsent(film.getId(), ConcurrentHashMap.newKeySet());
    }

    @Override
    public void delete(Integer id) {
        films.remove(id);
        likes.remove(id);
    }

    @Override
    public Optional<Film> findById(Integer id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> findAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        likes.computeIfAbsent(filmId, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        Set<Integer> filmLikes = likes.get(filmId);
        if (filmLikes != null) {
            filmLikes.remove(userId);
        }
    }

    @Override
    public long getLikeCount(Integer filmId) {
        Set<Integer> filmLikes = likes.get(filmId);
        return filmLikes == null ? 0 : filmLikes.size();
    }

    @Override
    public List<Film> getPopular(int count) {
        List<Film> filmsByPopularity = new ArrayList<>(films.values());
        filmsByPopularity.sort((a, b) -> {
            long la = getLikeCount(a.getId());
            long lb = getLikeCount(b.getId());
            int cmp = Long.compare(lb, la);
            if (cmp != 0) {
                return cmp;
            }
            return Integer.compare(
                    a.getId() == null ? Integer.MAX_VALUE : a.getId(),
                    b.getId() == null ? Integer.MAX_VALUE : b.getId());
        });
        return filmsByPopularity.stream().limit(count).toList();
    }
}
