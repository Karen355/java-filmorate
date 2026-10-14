package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedEvent {

    private Long timestamp;

    private Integer userId;

    private EventType eventType;

    private Operation operation;

    private Integer eventId;

    private Integer entityId;
}
