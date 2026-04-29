package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import project.server.entity.HeartRate;

public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {

}
