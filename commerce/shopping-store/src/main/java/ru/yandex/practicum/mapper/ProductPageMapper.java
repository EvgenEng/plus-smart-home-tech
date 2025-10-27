package ru.yandex.practicum.mapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ProductPageMapper {

    public Map<String, Object> toResponseMap(Page<?> resultPage) {
        Map<String, Object> response = new HashMap<>();

        response.put("content", resultPage.getContent());
        response.put("totalElements", resultPage.getTotalElements());
        response.put("totalPages", resultPage.getTotalPages());
        response.put("size", resultPage.getSize());
        response.put("number", resultPage.getNumber());
        response.put("first", resultPage.isFirst());
        response.put("last", resultPage.isLast());
        response.put("numberOfElements", resultPage.getNumberOfElements());
        response.put("empty", resultPage.isEmpty());

        List<Map<String, String>> sortArray = resultPage.getSort().stream()
                .map(order -> {
                    Map<String, String> sortObj = new HashMap<>();
                    sortObj.put("property", order.getProperty());
                    sortObj.put("direction", order.getDirection().name());
                    return sortObj;
                })
                .collect(Collectors.toList());

        response.put("sort", sortArray);

        return response;
    }
}
