package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryCostRequest;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.DeliveryState;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.ShippedToDeliveryRequest;
import ru.yandex.practicum.exception.NoDeliveryFoundException;
import ru.yandex.practicum.mapper.DeliveryMapper;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.repository.DeliveryRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final DeliveryMapper deliveryMapper;
    private final OrderClient orderClient;
    private final WarehouseClient warehouseClient;

    @Transactional
    public DeliveryDto planDelivery(DeliveryDto deliveryDto) {
        Delivery delivery = deliveryMapper.toEntity(deliveryDto);
        delivery.setDeliveryState(DeliveryState.CREATED);

        Delivery savedDelivery = deliveryRepository.save(delivery);
        log.info("Delivery planned: {} for order: {}", savedDelivery.getDeliveryId(), deliveryDto.getOrderId());

        return deliveryMapper.toDto(savedDelivery);
    }

    public Double deliveryCost(DeliveryCostRequest request) {
        if (request == null || request.getOrder() == null || request.getDeliveryAddress() == null) {
            throw new NoDeliveryFoundException(
                    "Order and delivery address are required for delivery cost calculation",
                    "Order information incomplete",
                    HttpStatus.BAD_REQUEST
            );
        }

        OrderDto orderDto = request.getOrder();
        AddressDto deliveryAddress = request.getDeliveryAddress();
        double baseCost = 5.0;
        double totalCost = baseCost;

        AddressDto warehouseAddress = warehouseClient.getWarehouseAddress();

        if (warehouseAddress.getStreet().contains("ADDRESS_2")) {
            totalCost = baseCost * 2 + baseCost; // 5*2 + 5 = 15
        } else if (warehouseAddress.getStreet().contains("ADDRESS_1")) {
            totalCost = baseCost * 1 + baseCost; // 5*1 + 5 = 10
        }

        if (Boolean.TRUE.equals(orderDto.getFragile())) {
            totalCost += totalCost * 0.2;
        }

        if (orderDto.getDeliveryWeight() != null) {
            totalCost += orderDto.getDeliveryWeight() * 0.3;
        }

        if (orderDto.getDeliveryVolume() != null) {
            totalCost += orderDto.getDeliveryVolume() * 0.2;
        }

        if (!warehouseAddress.getStreet().equals(deliveryAddress.getStreet())) {
            totalCost += totalCost * 0.2; // +20% за другой адрес
            log.info("Applied 20% surcharge for different street delivery");
        }

        log.info("Calculated delivery cost: {} for order: {}", totalCost, orderDto.getOrderId());
        return totalCost;
    }

    @Transactional
    public void deliveryPicked(UUID orderId) {
        Delivery delivery = getDeliveryByOrderId(orderId);
        delivery.setDeliveryState(DeliveryState.IN_PROGRESS);
        deliveryRepository.save(delivery);

        orderClient.assembly(orderId);

        ShippedToDeliveryRequest request = ShippedToDeliveryRequest.builder()
                .orderId(orderId)
                .deliveryId(delivery.getDeliveryId())
                .build();
        warehouseClient.shippedToDelivery(request);

        log.info("Delivery picked for order: {}", orderId);
    }

    @Transactional
    public void deliverySuccessful(UUID orderId) {
        Delivery delivery = getDeliveryByOrderId(orderId);
        delivery.setDeliveryState(DeliveryState.DELIVERED);
        deliveryRepository.save(delivery);

        orderClient.delivery(orderId);

        log.info("Delivery successful for order: {}", orderId);
    }

    @Transactional
    public void deliveryFailed(UUID orderId) {
        Delivery delivery = getDeliveryByOrderId(orderId);
        delivery.setDeliveryState(DeliveryState.FAILED);
        deliveryRepository.save(delivery);

        orderClient.deliveryFailed(orderId);

        log.info("Delivery failed for order: {}", orderId);
    }

    private Delivery getDeliveryByOrderId(UUID orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new NoDeliveryFoundException(
                        "Delivery not found for order: " + orderId,
                        "Delivery not found",
                        HttpStatus.NOT_FOUND
                ));
    }
}
