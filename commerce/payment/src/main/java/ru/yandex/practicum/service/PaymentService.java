package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderClient;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.PaymentState;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.mapper.PaymentMapper;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderClient orderClient;
    private final ShoppingStoreClient shoppingStoreClient;

    public Double productCost(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        double totalCost = 0.0;
        for (var entry : orderDto.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Integer quantity = entry.getValue();

            ProductDto product = shoppingStoreClient.getProduct(productId);
            totalCost += product.getPrice() * quantity;
        }

        log.info("Calculated product cost: {} for order: {}", totalCost, orderDto.getOrderId());
        return totalCost;
    }

    public Double getTotalCost(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        Double productCost = productCost(orderDto);

        Double vat = productCost * 0.1;

        Double deliveryCost = orderDto.getDeliveryPrice();
        if (deliveryCost == null) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Delivery cost is required for total calculation",
                    "Delivery information missing",
                    org.springframework.http.HttpStatus.BAD_REQUEST
            );
        }

        Double totalCost = productCost + vat + deliveryCost;

        log.info("Calculated total cost: {} for order: {} (products: {}, vat: {}, delivery: {})",
                totalCost, orderDto.getOrderId(), productCost, vat, deliveryCost);

        return totalCost;
    }

    @Transactional
    public PaymentDto payment(OrderDto orderDto) {
        validateOrderForCalculation(orderDto);

        Double productCost = productCost(orderDto);
        Double totalCost = getTotalCost(orderDto);
        Double vat = productCost * 0.1;
        Double deliveryCost = orderDto.getDeliveryPrice();

        Payment payment = Payment.builder()
                .orderId(orderDto.getOrderId())
                .state(PaymentState.PENDING)
                .totalPayment(totalCost)
                .deliveryTotal(deliveryCost)
                .feeTotal(vat)
                .productCost(productCost)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment created: {} for order: {}", savedPayment.getPaymentId(), orderDto.getOrderId());

        return paymentMapper.toDto(savedPayment);
    }

    @Transactional
    public void paymentSuccess(UUID paymentId) {
        Payment payment = getPaymentById(paymentId);
        payment.setState(PaymentState.SUCCESS);
        paymentRepository.save(payment);

        orderClient.payment(payment.getOrderId());

        log.info("Payment successful: {}", paymentId);
    }

    @Transactional
    public void paymentFailed(UUID paymentId) {
        Payment payment = getPaymentById(paymentId);
        payment.setState(PaymentState.FAILED);
        paymentRepository.save(payment);

        orderClient.paymentFailed(payment.getOrderId());

        log.info("Payment failed: {}", paymentId);
    }

    private void validateOrderForCalculation(OrderDto orderDto) {
        if (orderDto == null || orderDto.getProducts() == null || orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Order products are required for calculation",
                    "Order information incomplete",
                    org.springframework.http.HttpStatus.BAD_REQUEST
            );
        }
    }

    private Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException(
                        "Payment not found: " + paymentId,
                        "Payment not found",
                        org.springframework.http.HttpStatus.NOT_FOUND
                ));
    }
}
