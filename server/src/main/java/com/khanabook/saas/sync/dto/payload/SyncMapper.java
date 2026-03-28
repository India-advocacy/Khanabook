package com.khanabook.saas.sync.dto.payload;

import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.stream.Collectors;

public class SyncMapper {

    public static <S, T> T map(S source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map object", e);
        }
    }

    public static <S, T> List<T> mapList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> map(source, targetClass)).collect(Collectors.toList());
    }
}
