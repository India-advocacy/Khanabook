package com.khanabook.saas.sync.dto.payload;

import com.khanabook.saas.sync.entity.BaseSyncEntity;
import com.khanabook.saas.entity.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.stream.Collectors;

public class SyncMapper {

    /**
     * Maps Entity -> DTO (for Pull responses)
     */
    public static <S extends BaseSyncEntity, T> T map(S source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            
            // Explicitly map all core sync fields and relational IDs
            if (target instanceof CategoryDTO dto) {
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            } else if (target instanceof MenuItemDTO dto) {
                MenuItem entity = (MenuItem) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setCategoryId(entity.getCategoryId());
                dto.setServerCategoryId(entity.getServerCategoryId());
            } else if (target instanceof ItemVariantDTO dto) {
                ItemVariant entity = (ItemVariant) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setMenuItemId(entity.getMenuItemId());
                dto.setServerMenuItemId(entity.getServerMenuItemId());
            } else if (target instanceof BillDTO dto) {
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            } else if (target instanceof BillItemDTO dto) {
                BillItem entity = (BillItem) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setBillId(entity.getBillId());
                dto.setServerBillId(entity.getServerBillId());
                dto.setMenuItemId(entity.getMenuItemId());
                dto.setServerMenuItemId(entity.getServerMenuItemId());
                dto.setVariantId(entity.getVariantId());
                dto.setServerVariantId(entity.getServerVariantId());
            } else if (target instanceof BillPaymentDTO dto) {
                BillPayment entity = (BillPayment) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setBillId(entity.getBillId());
                dto.setServerBillId(entity.getServerBillId());
            } else if (target instanceof StockLogDTO dto) {
                StockLog entity = (StockLog) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setMenuItemId(entity.getMenuItemId());
                dto.setServerMenuItemId(entity.getServerMenuItemId());
                dto.setVariantId(entity.getVariantId());
                dto.setServerVariantId(entity.getServerVariantId());
            } else if (target instanceof RestaurantProfileDTO dto) {
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            } else if (target instanceof UserDTO dto) {
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            }

            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map Entity to DTO", e);
        }
    }

    public static <S extends BaseSyncEntity, T> List<T> mapList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> map(source, targetClass)).collect(Collectors.toList());
    }

    /**
     * Maps DTO -> Entity (for Push requests)
     */
    public static <S, T extends BaseSyncEntity> T mapToEntity(S source, Class<T> targetClass) {
        if (source == null) return null;
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);

            // Explicitly map all core sync fields and relational IDs back to entity
            if (source instanceof CategoryDTO dto) {
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            } else if (source instanceof MenuItemDTO dto) {
                MenuItem entity = (MenuItem) target;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setCategoryId(dto.getCategoryId());
                entity.setServerCategoryId(dto.getServerCategoryId());
            } else if (source instanceof ItemVariantDTO dto) {
                ItemVariant entity = (ItemVariant) target;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setMenuItemId(dto.getMenuItemId());
                entity.setServerMenuItemId(dto.getServerMenuItemId());
            } else if (source instanceof BillDTO dto) {
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            } else if (source instanceof BillItemDTO dto) {
                BillItem entity = (BillItem) target;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setBillId(dto.getBillId());
                entity.setServerBillId(dto.getServerBillId());
                entity.setMenuItemId(dto.getMenuItemId());
                entity.setServerMenuItemId(dto.getServerMenuItemId());
                entity.setVariantId(dto.getVariantId());
                entity.setServerVariantId(dto.getServerVariantId());
            } else if (source instanceof BillPaymentDTO dto) {
                BillPayment entity = (BillPayment) target;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setBillId(dto.getBillId());
                entity.setServerBillId(dto.getServerBillId());
            } else if (source instanceof StockLogDTO dto) {
                StockLog entity = (StockLog) target;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setMenuItemId(dto.getMenuItemId());
                entity.setServerMenuItemId(dto.getServerMenuItemId());
                entity.setVariantId(dto.getVariantId());
                entity.setServerVariantId(dto.getServerVariantId());
            } else if (source instanceof RestaurantProfileDTO dto) {
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            } else if (source instanceof UserDTO dto) {
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            }

            return target;
        } catch (Exception e) {
            throw new RuntimeException("Failed to map DTO to Entity", e);
        }
    }

    public static <S, T extends BaseSyncEntity> List<T> mapToEntityList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null) return null;
        return sourceList.stream().map(source -> mapToEntity(source, targetClass)).collect(Collectors.toList());
    }
}
