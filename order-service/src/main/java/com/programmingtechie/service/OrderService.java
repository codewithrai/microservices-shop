package com.programmingtechie.service;

import com.programmingtechie.dto.InventoryResponse;
import com.programmingtechie.dto.OrderLineItemsDTO;
import com.programmingtechie.dto.OrderRequest;
import com.programmingtechie.model.Order;
import com.programmingtechie.model.OrderLineItems;
import com.programmingtechie.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    private final WebClient.Builder webClientBuilder;

    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDTOS()
                .stream()
                .map(orderLineItemsDTO -> mapToDTO(orderLineItemsDTO, order)).toList();

        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream().map(OrderLineItems::getSkuCode).toList();

        // Call inventory service, and place order if product in stock
        InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();

        boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);

        if (allProductsInStock) {
            orderRepository.save(order);
            return "Order placed successfully";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please again later");
        }

    }

    private OrderLineItems mapToDTO(OrderLineItemsDTO orderLineItemsDTO, Order order) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDTO.getPrice());
        orderLineItems.setQuantity(orderLineItemsDTO.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDTO.getSkuCode());
        orderLineItems.setOrder(order);
        return orderLineItems;
    }
}
