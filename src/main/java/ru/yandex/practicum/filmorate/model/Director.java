package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Director {

    private Integer id;

    @NotBlank(message = "Имя режиссёра не может быть пустым")
    @Size(max = 100, message = "Имя режиссёра не может превышать 100 символов")
    private String name;
}
