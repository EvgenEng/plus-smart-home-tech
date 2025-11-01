// warehouse/src/main/java/ru/yandex/practicum/repository/OrderBookingRepository.java
package ru.yandex.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.model.OrderBooking;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderBookingRepository extends JpaRepository<OrderBooking, UUID> {
    Optional<OrderBooking> findByOrderId(UUID orderId);
}
