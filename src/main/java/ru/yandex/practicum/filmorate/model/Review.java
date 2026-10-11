package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Модель отзыва на фильм. Рейтинг полезности useful вычисляется по оценкам пользователей.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    private Integer reviewId;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    @Size(max = 2000, message = "Текст отзыва не может превышать 2000 символов")
    private String content;

    @NotNull(message = "Тип отзыва обязателен")
    private Boolean isPositive;

    @NotNull(message = "Пользователь обязателен")
    private Integer userId;

    @NotNull(message = "Фильм обязателен")
    private Integer filmId;

    private int useful;
}
