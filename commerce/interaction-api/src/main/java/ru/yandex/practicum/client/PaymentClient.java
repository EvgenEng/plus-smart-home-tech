package ru.yandex.practicum.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;

import java.util.UUID;

@FeignClient(name = "payment")
public interface PaymentClient {

    @PostMapping("/api/v1/payment/totalCost")
    Double getTotalCost(@RequestBody OrderDto orderDto);

    @PostMapping("/api/v1/payment/productCost")
    Double productCost(@RequestBody OrderDto orderDto);

    @PostMapping("/api/v1/payment")
    PaymentDto payment(@RequestBody OrderDto orderDto);

    @PostMapping("/api/v1/payment/refund")
    void paymentSuccess(@RequestBody UUID paymentId);

    @PostMapping("/api/v1/payment/failed")
    void paymentFailed(@RequestBody UUID paymentId);
}
