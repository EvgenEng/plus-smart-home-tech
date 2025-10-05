package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.Sensor;

import java.util.List;
import java.util.Optional;

@Repository
public interface SensorRepository extends JpaRepository<Sensor, String> {

    boolean existsByIdInAndHubId(List<String> ids, String hubId);

    void deleteByIdAndHubId(String id, String hubId);

    Optional<Sensor> findById(String id);

    @Query("SELECT s FROM Sensor s WHERE s.id IN :sensorIds AND s.hubId = :hubId")
    List<Sensor> findByIdInAndHubId(@Param("sensorIds") List<String> sensorIds, @Param("hubId") String hubId);

}
