package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.DeliveryClient;
import ru.yandex.practicum.client.PaymentClient;
import ru.yandex.practicum.client.WarehouseClient;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.CreateNewOrderRequest;
import ru.yandex.practicum.dto.DeliveryCostRequest;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.OrderState;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.ProductReturnRequest;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NotAuthorizedUserException;
import ru.yandex.practicum.mapper.OrderMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.repository.OrderRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;
    private final DeliveryClient deliveryClient;
    private final WarehouseClient warehouseClient;

    public List<OrderDto> getClientOrders(String username) {
        if (username == null || username.isBlank()) {
            throw new NotAuthorizedUserException("Username is required");
        }

        return orderRepository.findByUsername(username).stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderDto createNewOrder(CreateNewOrderRequest request, String username) {
        log.info("Creating new order for user: {}", username);

        warehouseClient.checkProductQuantityEnoughForShoppingCart(request.getShoppingCart().getProducts());

        BookedProductsDto bookedProducts = warehouseClient.checkProductQuantity(request.getShoppingCart());

        Order order = Order.builder()
                .shoppingCartId(request.getShoppingCart().getShoppingCartId())
                .products(request.getShoppingCart().getProducts())
                .state(OrderState.NEW)
                .deliveryWeight(bookedProducts.getDeliveryWeight())
                .deliveryVolume(bookedProducts.getDeliveryVolume())
                .fragile(bookedProducts.getFragile())
                .username(username)
                .build();

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully: {}", savedOrder.getOrderId());

        return orderMapper.toDto(savedOrder);
    }

    @Transactional
    public OrderDto payment(UUID orderId) {
        Order order = getOrderById(orderId);

        OrderDto orderDto = orderMapper.toDto(order);
        Double productCost = paymentClient.productCost(orderDto);
        order.setProductPrice(productCost);

        DeliveryCostRequest deliveryRequest = DeliveryCostRequest.builder()
                .order(orderDto)
                .deliveryAddress(new AddressDto())
                .build();
        Double deliveryCost = deliveryClient.deliveryCost(deliveryRequest);
        order.setDeliveryPrice(deliveryCost);

        Double totalCost = paymentClient.getTotalCost(orderDto);
        order.setTotalPrice(totalCost);

        PaymentDto payment = paymentClient.payment(orderDto);
        order.setPaymentId(payment.getPaymentId());
        order.setState(OrderState.ON_PAYMENT);

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setState(OrderState.PAYMENT_FAILED);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto delivery(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setState(OrderState.ON_DELIVERY);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setState(OrderState.DELIVERY_FAILED);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto assembly(UUID orderId) {
        Order order = getOrderById(orderId);

        AssemblyProductsForOrderRequest assemblyRequest = AssemblyProductsForOrderRequest.builder()
                .orderId(orderId)
                .products(order.getProducts())
                .build();

        warehouseClient.assemblyProductsForOrder(assemblyRequest);
        order.setState(OrderState.ASSEMBLED);

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setState(OrderState.ASSEMBLY_FAILED);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto complete(UUID orderId) {
        Order order = getOrderById(orderId);
        order.setState(OrderState.COMPLETED);
        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto calculateTotalCost(UUID orderId) {
        Order order = getOrderById(orderId);
        OrderDto orderDto = orderMapper.toDto(order);

        Double totalCost = paymentClient.getTotalCost(orderDto);
        order.setTotalPrice(totalCost);

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto calculateDeliveryCost(UUID orderId) {
        Order order = getOrderById(orderId);
        OrderDto orderDto = orderMapper.toDto(order);

        DeliveryCostRequest deliveryRequest = DeliveryCostRequest.builder()
                .order(orderDto)
                .deliveryAddress(new AddressDto())
                .build();
        Double deliveryCost = deliveryClient.deliveryCost(deliveryRequest);
        order.setDeliveryPrice(deliveryCost);

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = getOrderById(request.getOrderId());

        warehouseClient.acceptReturn(request.getProducts());
        order.setState(OrderState.PRODUCT_RETURNED);

        Order updatedOrder = orderRepository.save(order);
        return orderMapper.toDto(updatedOrder);
    }

    private Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoOrderFoundException(
                        "Order not found: " + orderId,
                        "Order not found",
                        HttpStatus.BAD_REQUEST
                ));
    }
}
