package com.khanabook.saas.sync.dto.payload;

import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.stream.Collectors;

public class SyncMapper {

    /**
     * Maps a source object to a target class.
     * Used for Pull (Entity -> DTO). id maps to serverId via @JsonProperty.
     */
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

    /**
     * Maps a list of sources to a list of target objects.
     */
    public static <S, T> List<T> mapList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> map(source, targetClass)).collect(Collectors.toList());
    }

    /**
     * Maps a DTO to an Entity, explicitly ignoring the 'id' field to let the DB generate it.
     * Used for Push (DTO -> Entity).
     */
    public static <S, T> T mapToEntity(S source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            // We ignore "id" because in Push, the DTO's "id" (aliased from serverId) 
            // is the client's knowledge of the server ID, but JPA/Hibernate 
            // should manage its own primary key or treat it as a detached entity if it exists.
            // For KhanaBook sync, we usually let the server decide or update by unique constraints.
            BeanUtils.copyProperties(source, target, "id");
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map DTO to Entity", e);
        }
    }

    /**
     * Maps a list of DTOs to a list of Entities, ignoring 'id'.
     */
    public static <S, T> List<T> mapToEntityList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> mapToEntity(source, targetClass)).collect(Collectors.toList());
    }
}
