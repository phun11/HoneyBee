package com.example.agritrace.repository;

import com.example.agritrace.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {
    private final JdbcTemplate jdbc;
    private final AuditRepository audit;

    public AuthRepository(JdbcTemplate jdbc, AuditRepository audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    private final RowMapper<User> mapper = (rs, i) -> {
        User u = new User();
        u.userId = rs.getLong("USER_ID");
        u.username = rs.getString("USERNAME");
        u.fullName = rs.getString("FULL_NAME");
        u.email = rs.getString("EMAIL");
        u.phone = rs.getString("PHONE");
        u.role = rs.getString("ROLE");
        Object farmId = rs.getObject("FARM_ID");
        Object transporterId = rs.getObject("TRANSPORTER_ID");
        Object storeId = rs.getObject("STORE_ID");
        u.farmId = farmId == null ? null : ((Number) farmId).longValue();
        u.transporterId = transporterId == null ? null : ((Number) transporterId).longValue();
        u.storeId = storeId == null ? null : ((Number) storeId).longValue();
        u.status = rs.getString("STATUS");
        return u;
    };

    public User login(String username, String password, String ip) {
        User user = jdbc.queryForObject(
                "SELECT * FROM USERS WHERE USERNAME=? AND PASSWORD=? AND STATUS='ACTIVE'",
                mapper, username, password);
        audit.log("USERS", String.valueOf(user.userId), "LOGIN", null, "LOGIN OK role=" + user.role, user.username, ip);
        return user;
    }
}
