package com.example.test.cache;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TruckCountCache {
    // Key: jobRotationId, Value: số xe rác nhỏ (smallTrucksCount)
    private final Map<Integer, Integer> cache = new ConcurrentHashMap<>();

    // Thêm/update cache
    public void put(Integer jobRotationId, Integer smallTrucksCount) {
        cache.put(jobRotationId, smallTrucksCount);
    }

    // Lấy giá trị từ cache
    public Integer get(Integer jobRotationId) {
        return cache.get(jobRotationId);
    }

    // Xóa khỏi cache khi không cần
    public void evict(Integer jobRotationId) {
        cache.remove(jobRotationId);
    }
}
