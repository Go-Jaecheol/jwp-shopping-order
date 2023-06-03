package cart.infrastructure.dao;

import cart.entity.MemberCouponEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemberCouponDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<MemberCouponEntity> rowMapper = (rs, rowNum) ->
            MemberCouponEntity.of(
                    rs.getLong("id"),
                    rs.getBoolean("is_used"),
                    rs.getLong("member_id"),
                    rs.getLong("coupon_id")
            );

    public MemberCouponDao(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<MemberCouponEntity> findByCouponIdAndMemberId(final Long couponId, final Long memberId) {
        final String sql = "SELECT * FROM member_coupon WHERE coupon_id = ? AND member_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, rowMapper, couponId, memberId));
        } catch (EmptyResultDataAccessException exception) {
            return Optional.empty();
        }
    }

    public void update(final MemberCouponEntity coupon) {
        final String sql = "UPDATE member_coupon SET is_used = ? WHERE coupon_id = ? AND member_id = ?";
        jdbcTemplate.update(sql, coupon.isUsed(), coupon.getCouponId(), coupon.getMemberId());
    }

    public List<Long> findCouponIdsByMemberId(final Long memberId) {
        final String sql = "SELECT coupon_id FROM member_coupon WHERE member_id = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong("coupon_id"), memberId);
    }
}
