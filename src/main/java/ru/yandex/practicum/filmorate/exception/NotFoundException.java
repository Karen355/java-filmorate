package ru.yandex.practicum.filmorate.exception;

/**
 * Исключение при отсутствии сущности по идентификатору.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
