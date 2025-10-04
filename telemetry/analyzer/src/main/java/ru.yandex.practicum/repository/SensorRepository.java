package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.entity.Sensor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {
    boolean existsByIdAndHubId(String id, String hubId);

    @Query("SELECT s FROM Sensor s WHERE s.id IN :ids AND s.hubId = :hubId")
    List<Sensor> findByIdInAndHubId(@Param("ids") Collection<String> ids, @Param("hubId") String hubId);

    Optional<Sensor> findByIdAndHubId(String id, String hubId);
}
