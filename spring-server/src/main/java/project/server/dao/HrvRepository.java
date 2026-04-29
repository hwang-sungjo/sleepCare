package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.Hrv;

public interface HrvRepository extends JpaRepository<Hrv, Long> {
}
