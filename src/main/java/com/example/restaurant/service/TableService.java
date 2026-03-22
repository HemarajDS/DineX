package com.example.restaurant.service;

import com.example.restaurant.dto.TableRequest;
import com.example.restaurant.dto.TableResponse;
import com.example.restaurant.entity.Table;
import com.example.restaurant.exception.BadRequestException;
import com.example.restaurant.exception.ResourceNotFoundException;
import com.example.restaurant.repository.TableRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TableService {

    private final TableRepository tableRepository;

    public TableService(TableRepository tableRepository) {
        this.tableRepository = tableRepository;
    }

    public List<TableResponse> getTables() {
        return tableRepository.findAll().stream()
                .sorted((left, right) -> left.getTableNumber().compareToIgnoreCase(right.getTableNumber()))
                .map(this::mapToResponse)
                .toList();
    }

    public TableResponse getTableById(String id) {
        Table table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
        return mapToResponse(table);
    }

    public TableResponse createTable(TableRequest request) {
        tableRepository.findByTableNumberIgnoreCase(request.getTableNumber().trim())
                .ifPresent(existing -> {
                    throw new BadRequestException("Table number already exists");
                });

        Table table = new Table();
        table.setTableNumber(request.getTableNumber().trim().toUpperCase());
        table.setCapacity(request.getCapacity());
        table.setOccupied(request.isOccupied());
        return mapToResponse(tableRepository.save(table));
    }

    public TableResponse updateTable(String id, TableRequest request) {
        Table table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));

        String normalizedTableNumber = request.getTableNumber().trim().toUpperCase();
        if (tableRepository.existsByTableNumberIgnoreCaseAndIdNot(normalizedTableNumber, id)) {
            throw new BadRequestException("Table number already exists");
        }

        table.setTableNumber(normalizedTableNumber);
        table.setCapacity(request.getCapacity());
        table.setOccupied(request.isOccupied());
        return mapToResponse(tableRepository.save(table));
    }

    public TableResponse updateTableStatus(String id, boolean occupied) {
        Table table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
        table.setOccupied(occupied);
        return mapToResponse(tableRepository.save(table));
    }

    public void deleteTable(String id, boolean hasOpenOrders) {
        Table table = tableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));

        if (table.isOccupied()) {
            throw new BadRequestException("Occupied tables cannot be deleted");
        }

        if (hasOpenOrders) {
            throw new BadRequestException("Tables with open orders cannot be deleted");
        }

        tableRepository.delete(table);
    }

    public TableResponse updateTableStatusByNumber(String tableNumber, boolean occupied) {
        Table table = tableRepository.findByTableNumberIgnoreCase(tableNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with number: " + tableNumber));
        table.setOccupied(occupied);
        return mapToResponse(tableRepository.save(table));
    }

    private TableResponse mapToResponse(Table table) {
        return new TableResponse(table.getId(), table.getTableNumber(), table.getCapacity(), table.isOccupied());
    }
}
