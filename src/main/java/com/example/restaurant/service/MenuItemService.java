package com.example.restaurant.service;

import com.example.restaurant.dto.MenuItemRequest;
import com.example.restaurant.dto.MenuItemResponse;
import com.example.restaurant.entity.MenuItem;
import com.example.restaurant.exception.BadRequestException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.MenuItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class MenuItemService {

    private static final Set<String> ALLOWED_CATEGORIES = Set.of("Starters", "Mains", "Desserts", "Beverages");
    private static final Set<String> ALLOWED_TYPES = Set.of("Signature", "Classic", "Premium", "Chef Special");

    private final MenuItemRepository menuItemRepository;

    public MenuItemService(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    public List<MenuItemResponse> getAllMenuItems(String search, String category) {
        String safeSearch = search == null ? "" : search.trim();
        String safeCategory = category == null ? "" : category.trim();

        return menuItemRepository.findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(safeSearch, safeCategory)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        validateCatalogValues(request.getCategory(), request.getType());
        MenuItem menuItem = new MenuItem();
        menuItem.setName(request.getName().trim());
        menuItem.setPrice(request.getPrice());
        menuItem.setType(request.getType().trim());
        menuItem.setCategory(request.getCategory().trim());
        menuItem.setImageUrl(request.getImageUrl().trim());
        menuItem.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        menuItem.setAvailable(request.isAvailable());
        return mapToResponse(menuItemRepository.save(menuItem));
    }

    public MenuItemResponse updateMenuItem(String id, MenuItemRequest request) {
        validateCatalogValues(request.getCategory(), request.getType());
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));

        menuItem.setName(request.getName().trim());
        menuItem.setPrice(request.getPrice());
        menuItem.setType(request.getType().trim());
        menuItem.setCategory(request.getCategory().trim());
        menuItem.setImageUrl(request.getImageUrl().trim());
        menuItem.setDescription(request.getDescription() == null ? "" : request.getDescription().trim());
        menuItem.setAvailable(request.isAvailable());
        return mapToResponse(menuItemRepository.save(menuItem));
    }

    public void deleteMenuItem(String id) {
        MenuItem menuItem = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
        menuItemRepository.delete(menuItem);
    }

    public MenuItem getMenuItemEntity(String id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + id));
    }

    private MenuItemResponse mapToResponse(MenuItem menuItem) {
        return new MenuItemResponse(
                menuItem.getId(),
                menuItem.getName(),
                menuItem.getPrice(),
                menuItem.getType(),
                menuItem.getCategory(),
                menuItem.getImageUrl(),
                menuItem.getDescription(),
                menuItem.isAvailable()
        );
    }

    private void validateCatalogValues(String category, String type) {
        if (!ALLOWED_CATEGORIES.contains(category.trim())) {
            throw new BadRequestException("Invalid category. Allowed values: " + ALLOWED_CATEGORIES);
        }
        if (!ALLOWED_TYPES.contains(type.trim())) {
            throw new BadRequestException("Invalid type. Allowed values: " + ALLOWED_TYPES);
        }
    }
}
