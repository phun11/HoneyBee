package com.example.agritrace.repository;

import com.example.agritrace.model.Farm;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class FarmRepository {
    private final JdbcTemplate jdbc;
    private final AuditRepository audit;
    public FarmRepository(JdbcTemplate jdbc, AuditRepository audit) { this.jdbc = jdbc; this.audit = audit; }

    private final RowMapper<Farm> mapper = (rs, i) -> {
        Farm f = new Farm();
        f.farmId = rs.getLong("FARM_ID");
        f.farmName = rs.getString("FARM_NAME");
        f.address = rs.getString("ADDRESS");
        f.ownerName = rs.getString("OWNER_NAME");
        f.certification = rs.getString("CERTIFICATION");
        f.contactPhone = rs.getString("CONTACT_PHONE");
        f.contactEmail = rs.getString("CONTACT_EMAIL");
        f.status = rs.getString("STATUS");
        return f;
    };

    public List<Farm> findAll() { return jdbc.query("SELECT * FROM FARMS ORDER BY FARM_ID", mapper); }
    public Farm findById(Long id) { return jdbc.queryForObject("SELECT * FROM FARMS WHERE FARM_ID=?", mapper, id); }

    public void create(Farm f) {
        jdbc.update("INSERT INTO FARMS(FARM_NAME, ADDRESS, OWNER_NAME, CERTIFICATION, CONTACT_PHONE, CONTACT_EMAIL) VALUES(?,?,?,?,?,?)",
                f.farmName, f.address, f.ownerName, f.certification, f.contactPhone, f.contactEmail);
        audit.log("FARMS", f.farmName, "INSERT", null, f.address, "system", null);
    }

    public void update(Long id, Farm f) {
        jdbc.update("UPDATE FARMS SET FARM_NAME=?, ADDRESS=?, OWNER_NAME=?, CERTIFICATION=?, CONTACT_PHONE=?, CONTACT_EMAIL=?, UPDATED_AT=CURRENT_TIMESTAMP WHERE FARM_ID=?",
                f.farmName, f.address, f.ownerName, f.certification, f.contactPhone, f.contactEmail, id);
        audit.log("FARMS", String.valueOf(id), "UPDATE", null, f.farmName, "system", null);
    }

    public void delete(Long id) {
        jdbc.update("UPDATE FARMS SET STATUS='INACTIVE', UPDATED_AT=CURRENT_TIMESTAMP WHERE FARM_ID=?", id);
        audit.log("FARMS", String.valueOf(id), "UPDATE", null, "INACTIVE", "system", null);
    }
}
