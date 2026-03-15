package ru.yandex.practicum.filmorate.exception;

/**
 * Исключение при ошибке валидации данных.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
