package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.mapper.ReviewRowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private static final String SELECT_REVIEWS = """
            SELECT r.id, r.content, r.is_positive, r.user_id, r.film_id, COALESCE(rr.useful, 0) AS useful
            FROM reviews r
            LEFT JOIN (SELECT review_id, SUM(CASE WHEN is_useful THEN 1 ELSE -1 END) AS useful
                       FROM review_reactions GROUP BY review_id) rr
                ON rr.review_id = r.id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ReviewRowMapper rowMapper = new ReviewRowMapper();

    @Override
    public Review create(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, new String[]{"id"});
            statement.setString(1, review.getContent());
            statement.setBoolean(2, review.getIsPositive());
            statement.setInt(3, review.getUserId());
            statement.setInt(4, review.getFilmId());
            return statement;
        }, keyHolder);
        review.setReviewId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        review.setUseful(0);
        return review;
    }

    @Override
    public void update(Review review) {
        int updated = jdbcTemplate.update("UPDATE reviews SET content = ?, is_positive = ? WHERE id = ?",
                review.getContent(), review.getIsPositive(), review.getReviewId());
        if (updated == 0) {
            throw new NotFoundException("Отзыв с id=" + review.getReviewId() + " не найден");
        }
    }

    @Override
    public void delete(Integer id) {
        jdbcTemplate.update("DELETE FROM reviews WHERE id = ?", id);
    }

    @Override
    public Optional<Review> findById(Integer id) {
        return jdbcTemplate.query(SELECT_REVIEWS + "WHERE r.id = ?", rowMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public List<Review> findAll(Integer filmId, int count) {
        String order = "ORDER BY useful DESC, r.id LIMIT ?";
        if (filmId == null) {
            return jdbcTemplate.query(SELECT_REVIEWS + order, rowMapper, count);
        }
        return jdbcTemplate.query(SELECT_REVIEWS + "WHERE r.film_id = ? " + order, rowMapper, filmId, count);
    }

    @Override
    public void addReaction(Integer reviewId, Integer userId, boolean isUseful) {
        jdbcTemplate.update("""
                MERGE INTO review_reactions (review_id, user_id, is_useful) KEY (review_id, user_id)
                VALUES (?, ?, ?)
                """, reviewId, userId, isUseful);
    }

    @Override
    public void removeReaction(Integer reviewId, Integer userId, boolean isUseful) {
        jdbcTemplate.update("DELETE FROM review_reactions WHERE review_id = ? AND user_id = ? AND is_useful = ?",
                reviewId, userId, isUseful);
    }
}
