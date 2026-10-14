package ru.yandex.practicum.filmorate.storage.feed;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.FeedEvent;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.mapper.FeedEventRowMapper;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class FeedDbStorage implements FeedStorage {

    private final JdbcTemplate jdbcTemplate;
    private final FeedEventRowMapper rowMapper = new FeedEventRowMapper();

    @Override
    public void addEvent(Integer userId, EventType eventType, Operation operation, Integer entityId) {
        String sql = """
                INSERT INTO feed_events (event_timestamp, user_id, event_type, operation, entity_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        jdbcTemplate.update(sql, System.currentTimeMillis(), userId, eventType.name(), operation.name(), entityId);
    }

    @Override
    public List<FeedEvent> getFeed(Integer userId) {
        String sql = """
                SELECT event_id, event_timestamp, user_id, event_type, operation, entity_id
                FROM feed_events
                WHERE user_id = ?
                ORDER BY event_id
                """;
        return jdbcTemplate.query(sql, rowMapper, userId);
    }
}
