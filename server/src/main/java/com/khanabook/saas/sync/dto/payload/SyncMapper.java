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
            
            // Core sync fields mapping
            if (target instanceof CategoryDTO) {
                CategoryDTO dto = (CategoryDTO) target;
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            } else if (target instanceof MenuItemDTO) {
                MenuItemDTO dto = (MenuItemDTO) target;
                MenuItem entity = (MenuItem) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setCategoryId(entity.getCategoryId());
                dto.setServerCategoryId(entity.getServerCategoryId());
            } else if (target instanceof ItemVariantDTO) {
                ItemVariantDTO dto = (ItemVariantDTO) target;
                ItemVariant entity = (ItemVariant) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setMenuItemId(entity.getMenuItemId());
                dto.setServerMenuItemId(entity.getServerMenuItemId());
            } else if (target instanceof BillDTO) {
                BillDTO dto = (BillDTO) target;
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            } else if (target instanceof BillItemDTO) {
                BillItemDTO dto = (BillItemDTO) target;
                BillItem entity = (BillItem) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setBillId(entity.getBillId());
                dto.setMenuItemId(entity.getMenuItemId());
                dto.setVariantId(entity.getVariantId());
                dto.setServerBillId(entity.getServerBillId());
                dto.setServerMenuItemId(entity.getServerMenuItemId());
                dto.setServerVariantId(entity.getServerVariantId());
            } else if (target instanceof BillPaymentDTO) {
                BillPaymentDTO dto = (BillPaymentDTO) target;
                BillPayment entity = (BillPayment) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setBillId(entity.getBillId());
                dto.setServerBillId(entity.getServerBillId());
            } else if (target instanceof StockLogDTO) {
                StockLogDTO dto = (StockLogDTO) target;
                StockLog entity = (StockLog) source;
                dto.setId(entity.getId());
                dto.setLocalId(entity.getLocalId());
                dto.setServerUpdatedAt(entity.getServerUpdatedAt());
                dto.setMenuItemId(entity.getMenuItemId());
                dto.setVariantId(entity.getVariantId());
                dto.setServerMenuItemId(entity.getServerMenuItemId());
                dto.setServerVariantId(entity.getServerVariantId());
            } else if (target instanceof RestaurantProfileDTO) {
                RestaurantProfileDTO dto = (RestaurantProfileDTO) target;
                dto.setId(source.getId());
                dto.setLocalId(source.getLocalId());
                dto.setServerUpdatedAt(source.getServerUpdatedAt());
            } else if (target instanceof UserDTO) {
                UserDTO dto = (UserDTO) target;
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

            // Manual overrides for core sync fields
            if (source instanceof CategoryDTO) {
                CategoryDTO dto = (CategoryDTO) source;
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            } else if (source instanceof MenuItemDTO) {
                MenuItem entity = (MenuItem) target;
                MenuItemDTO dto = (MenuItemDTO) source;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setCategoryId(dto.getCategoryId());
                entity.setServerCategoryId(dto.getServerCategoryId());
            } else if (source instanceof ItemVariantDTO) {
                ItemVariant entity = (ItemVariant) target;
                ItemVariantDTO dto = (ItemVariantDTO) source;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setMenuItemId(dto.getMenuItemId());
                entity.setServerMenuItemId(dto.getServerMenuItemId());
            } else if (source instanceof BillDTO) {
                BillDTO dto = (BillDTO) source;
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            } else if (source instanceof BillItemDTO) {
                BillItem entity = (BillItem) target;
                BillItemDTO dto = (BillItemDTO) source;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setBillId(dto.getBillId());
                entity.setMenuItemId(dto.getMenuItemId());
                entity.setVariantId(dto.getVariantId());
                entity.setServerBillId(dto.getServerBillId());
                entity.setServerMenuItemId(dto.getServerMenuItemId());
                entity.setServerVariantId(dto.getServerVariantId());
            } else if (source instanceof BillPaymentDTO) {
                BillPayment entity = (BillPayment) target;
                BillPaymentDTO dto = (BillPaymentDTO) source;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setBillId(dto.getBillId());
                entity.setServerBillId(dto.getServerBillId());
            } else if (source instanceof StockLogDTO) {
                StockLog entity = (StockLog) target;
                StockLogDTO dto = (StockLogDTO) source;
                entity.setId(dto.getId());
                entity.setLocalId(dto.getLocalId());
                entity.setServerUpdatedAt(dto.getServerUpdatedAt());
                entity.setMenuItemId(dto.getMenuItemId());
                entity.setVariantId(dto.getVariantId());
                entity.setServerMenuItemId(dto.getServerMenuItemId());
                entity.setServerVariantId(dto.getServerVariantId());
            } else if (source instanceof RestaurantProfileDTO) {
                RestaurantProfileDTO dto = (RestaurantProfileDTO) source;
                target.setId(dto.getId());
                target.setLocalId(dto.getLocalId());
                target.setServerUpdatedAt(dto.getServerUpdatedAt());
            } else if (source instanceof UserDTO) {
                UserDTO dto = (UserDTO) source;
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
