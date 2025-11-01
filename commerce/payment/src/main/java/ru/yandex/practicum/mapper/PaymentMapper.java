package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.model.Payment;

@Component
public class PaymentMapper {
    public PaymentDto toDto(Payment payment) {
        return PaymentDto.builder()
                .paymentId(payment.getPaymentId())
                .totalPayment(payment.getTotalPayment())
                .deliveryTotal(payment.getDeliveryTotal())
                .feeTotal(payment.getFeeTotal())
                .build();
    }

    public Payment toEntity(PaymentDto paymentDto) {
        return Payment.builder()
                .paymentId(paymentDto.getPaymentId())
                .totalPayment(paymentDto.getTotalPayment())
                .deliveryTotal(paymentDto.getDeliveryTotal())
                .feeTotal(paymentDto.getFeeTotal())
                .build();
    }
}
