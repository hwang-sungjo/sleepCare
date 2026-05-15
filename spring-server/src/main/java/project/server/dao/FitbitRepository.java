package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.FitbitEntity;

public interface FitbitRepository extends JpaRepository<FitbitEntity, Long> {
}
