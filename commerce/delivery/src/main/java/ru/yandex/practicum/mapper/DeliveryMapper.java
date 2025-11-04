package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.model.Delivery;
import ru.yandex.practicum.model.Address;

@Component
public class DeliveryMapper {
    public DeliveryDto toDto(Delivery delivery) {
        return DeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .fromAddress(toAddressDto(delivery.getFromAddress()))
                .toAddress(toAddressDto(delivery.getToAddress()))
                .orderId(delivery.getOrderId())
                .deliveryState(delivery.getDeliveryState())
                .weight(delivery.getWeight())
                .volume(delivery.getVolume())
                .fragile(delivery.getFragile())
                .calculatedCost(delivery.getCalculatedCost())
                .build();
    }

    public Delivery toEntity(DeliveryDto deliveryDto) {
        return Delivery.builder()
                .deliveryId(deliveryDto.getDeliveryId())
                .fromAddress(toAddress(deliveryDto.getFromAddress()))
                .toAddress(toAddress(deliveryDto.getToAddress()))
                .orderId(deliveryDto.getOrderId())
                .deliveryState(deliveryDto.getDeliveryState())
                .weight(deliveryDto.getWeight())
                .volume(deliveryDto.getVolume())
                .fragile(deliveryDto.getFragile())
                .calculatedCost(deliveryDto.getCalculatedCost())
                .build();
    }

    private AddressDto toAddressDto(Address address) {
        if (address == null) return null;
        return AddressDto.builder()
                .country(address.getCountry())
                .city(address.getCity())
                .street(address.getStreet())
                .house(address.getHouse())
                .flat(address.getFlat())
                .build();
    }

    private Address toAddress(AddressDto addressDto) {
        if (addressDto == null) return null;
        return Address.builder()
                .country(addressDto.getCountry())
                .city(addressDto.getCity())
                .street(addressDto.getStreet())
                .house(addressDto.getHouse())
                .flat(addressDto.getFlat())
                .build();
    }
}
