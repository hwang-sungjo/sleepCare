package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.SpO2;

public interface SpO2Repository extends JpaRepository<SpO2, Long> {
}
