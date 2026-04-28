package project.server.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import project.server.dao.entity.FitbitEntity;

import java.util.Optional;

public interface FitbitRepository extends JpaRepository<FitbitEntity, String> {

    Optional<FitbitEntity> findByUserId(long userId);
}
