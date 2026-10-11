package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface DirectorStorage {

    Director create(Director director);

    void update(Director director);

    void delete(Integer id);

    Optional<Director> findById(Integer id);

    List<Director> findAll();

    List<Director> findAllByIds(Set<Integer> ids);
}
