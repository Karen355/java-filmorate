package ru.yandex.practicum.filmorate.storage.mapper;

import org.springframework.jdbc.core.RowMapper;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FeedEventRowMapper implements RowMapper<FeedEvent> {

    @Override
    public FeedEvent mapRow(ResultSet resultSet, int rowNum) throws SQLException {
        return FeedEvent.builder()
                .eventId(resultSet.getInt("event_id"))
                .timestamp(resultSet.getLong("event_timestamp"))
                .userId(resultSet.getInt("user_id"))
                .eventType(EventType.valueOf(resultSet.getString("event_type")))
                .operation(Operation.valueOf(resultSet.getString("operation")))
                .entityId(resultSet.getInt("entity_id"))
                .build();
    }
}
