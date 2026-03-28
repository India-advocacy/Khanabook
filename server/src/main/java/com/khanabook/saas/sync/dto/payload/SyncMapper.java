package com.khanabook.saas.sync.dto.payload;

import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.stream.Collectors;

public class SyncMapper {

    /**
     * Maps Entity -> DTO (for Pull responses)
     * Entity.id will map to DTO.id (aliased to serverId in JSON)
     */
    public static <S, T> T map(S source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map Entity to DTO", e);
        }
    }

    public static <S, T> List<T> mapList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> map(source, targetClass)).collect(Collectors.toList());
    }

    /**
     * Maps DTO -> Entity (for Push requests)
     * DTO.id (which is serverId from JSON) maps to Entity.id
     */
    public static <S, T> T mapToEntity(S source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            // Here DTO.id (serverId) DOES map to Entity.id
            // GenericSyncService handles the logic of whether to use this ID for merging.
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map DTO to Entity", e);
        }
    }

    public static <S, T> List<T> mapToEntityList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> mapToEntity(source, targetClass)).collect(Collectors.toList());
    }
}
