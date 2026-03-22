package com.example.restaurant.config;

import com.example.restaurant.entity.MenuItem;
import com.example.restaurant.entity.Order;
import com.example.restaurant.entity.OrderItem;
import com.example.restaurant.entity.OrderItemStatus;
import com.example.restaurant.entity.OrderStatus;
import com.example.restaurant.entity.Role;
import com.example.restaurant.entity.Table;
import com.example.restaurant.entity.User;
import com.example.restaurant.repository.MenuItemRepository;
import com.example.restaurant.repository.OrderRepository;
import com.example.restaurant.repository.TableRepository;
import com.example.restaurant.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final TableRepository tableRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            MenuItemRepository menuItemRepository,
            OrderRepository orderRepository,
            TableRepository tableRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.tableRepository = tableRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0 || menuItemRepository.count() > 0 || orderRepository.count() > 0 || tableRepository.count() > 0) {
            return;
        }

        seedUsers();
        seedTables();
        seedMenu();
        seedOrders();
    }

    private void seedUsers() {
        User admin = buildUser("admin-user-1", "Admin User", "admin@restaurant.com", Role.ADMIN, List.of());
        User customer = buildUser("customer-user-1", "Aarav Guest", "customer@restaurant.com", Role.CUSTOMER, List.of("T-04"));
        User waiter = buildUser("waiter-user-1", "Service Runner", "waiter@restaurant.com", Role.WAITER, List.of("T-01", "T-02", "T-03", "T-04"));
        User kitchen = buildUser("kitchen-user-1", "Chef Marco", "kitchen@restaurant.com", Role.KITCHEN, List.of());
        User cashier = buildUser("cashier-user-1", "Billing Desk", "cashier@restaurant.com", Role.CASHIER, List.of());

        userRepository.saveAll(List.of(admin, customer, waiter, kitchen, cashier));
    }

    private void seedTables() {
        tableRepository.saveAll(List.of(
                buildTable("table-1", "T-01", 2, false),
                buildTable("table-2", "T-02", 4, true),
                buildTable("table-3", "T-03", 4, false),
                buildTable("table-4", "T-04", 6, true),
                buildTable("table-5", "VIP-01", 6, false),
                buildTable("table-6", "VIP-02", 8, true)
        ));
    }

    private void seedMenu() {
        menuItemRepository.saveAll(List.of(
                buildMenuItem("menu-1", "Seared Tuna Tataki", new BigDecimal("980.00"), "Signature", "Starters", "https://images.unsplash.com/photo-1559742811-822873691df8?w=900&q=80", "Silky tuna slices finished with sesame and ponzu glaze.", true),
                buildMenuItem("menu-2", "Wild Mushroom Veloute", new BigDecimal("620.00"), "Chef Special", "Starters", "https://images.unsplash.com/photo-1547592166-23ac45744acd?w=900&q=80", "A deep earthy soup with truffle oil and herb foam.", true),
                buildMenuItem("menu-3", "Wagyu Beef Tenderloin", new BigDecimal("3200.00"), "Premium", "Mains", "https://images.unsplash.com/photo-1544025162-d76694265947?w=900&q=80", "Charcoal-seared tenderloin with smoked jus.", true),
                buildMenuItem("menu-4", "Herb Butter Sea Bass", new BigDecimal("1890.00"), "Signature", "Mains", "https://images.unsplash.com/photo-1559847844-5315695dadae?w=900&q=80", "Crisp skin sea bass with lemon beurre blanc.", true),
                buildMenuItem("menu-5", "Truffle Linguine", new BigDecimal("1280.00"), "Classic", "Mains", "https://images.unsplash.com/photo-1621996346565-e3dbc646d9a9?w=900&q=80", "House pasta finished with truffle cream and parmesan.", true),
                buildMenuItem("menu-6", "Dark Chocolate Fondant", new BigDecimal("650.00"), "Chef Special", "Desserts", "https://images.unsplash.com/photo-1624353365286-3f8d62daad51?w=900&q=80", "Molten center served with vanilla bean gelato.", true),
                buildMenuItem("menu-7", "Saffron Creme Brulee", new BigDecimal("540.00"), "Classic", "Desserts", "https://images.unsplash.com/photo-1470124182917-cc6e71b22ecc?w=900&q=80", "Velvety custard with a saffron caramel top.", true),
                buildMenuItem("menu-8", "Smoked Citrus Mocktail", new BigDecimal("380.00"), "Signature", "Beverages", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?w=900&q=80", "Smoked orange, tonic pearls, and rosemary aroma.", true),
                buildMenuItem("menu-9", "Aged Single Malt", new BigDecimal("1800.00"), "Premium", "Beverages", "https://images.unsplash.com/photo-1569529465841-dfecdab7503b?w=900&q=80", "Slow-sipped oak richness with honeyed finish.", true),
                buildMenuItem("menu-10", "Jasmine Tea Elixir", new BigDecimal("420.00"), "Classic", "Beverages", "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?w=900&q=80", "Warm jasmine infusion with citrus blossom notes.", false)
        ));
    }

    private void seedOrders() {
        MenuItem tuna = menuItemRepository.findById("menu-1").orElseThrow();
        MenuItem pasta = menuItemRepository.findById("menu-5").orElseThrow();
        MenuItem mocktail = menuItemRepository.findById("menu-8").orElseThrow();
        MenuItem fondant = menuItemRepository.findById("menu-6").orElseThrow();

        Order firstOrder = new Order();
        firstOrder.setId("order-1");
        firstOrder.setOrderCode("ORD-20260321-0001");
        firstOrder.setUserId("customer-user-1");
        firstOrder.setCustomerName("Aarav Guest");
        firstOrder.setTableNumber("T-04");
        firstOrder.setTotalAmount(new BigDecimal("2010.00"));
        firstOrder.setStatus(OrderStatus.PENDING);
        firstOrder.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        firstOrder.setOrderItems(List.of(
                buildOrderItem("oi-1", tuna.getId(), tuna.getName(), 1, tuna.getPrice()),
                buildOrderItem("oi-2", pasta.getId(), pasta.getName(), 1, pasta.getPrice()),
                buildOrderItem("oi-3", mocktail.getId(), mocktail.getName(), 2, mocktail.getPrice())
        ));

        Order secondOrder = new Order();
        secondOrder.setId("order-2");
        secondOrder.setOrderCode("ORD-20260321-0002");
        secondOrder.setUserId("customer-user-1");
        secondOrder.setCustomerName("Aarav Guest");
        secondOrder.setTableNumber("T-04");
        secondOrder.setTotalAmount(new BigDecimal("650.00"));
        secondOrder.setStatus(OrderStatus.PREPARING);
        secondOrder.setCreatedAt(LocalDateTime.now().minusMinutes(12));
        secondOrder.setOrderItems(List.of(
                buildOrderItem("oi-4", fondant.getId(), fondant.getName(), 1, fondant.getPrice())
        ));

        orderRepository.saveAll(List.of(firstOrder, secondOrder));
    }

    private User buildUser(String id, String name, String email, Role role, List<String> assignedTables) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(role);
        user.setAssignedTableNumbers(assignedTables);
        return user;
    }

    private Table buildTable(String id, String tableNumber, int capacity, boolean occupied) {
        Table table = new Table();
        table.setId(id);
        table.setTableNumber(tableNumber);
        table.setCapacity(capacity);
        table.setOccupied(occupied);
        return table;
    }

    private MenuItem buildMenuItem(String id, String name, BigDecimal price, String type, String category, String imageUrl, String description, boolean available) {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(id);
        menuItem.setName(name);
        menuItem.setPrice(price);
        menuItem.setType(type);
        menuItem.setCategory(category);
        menuItem.setImageUrl(imageUrl);
        menuItem.setDescription(description);
        menuItem.setAvailable(available);
        return menuItem;
    }

    private OrderItem buildOrderItem(String id, String menuItemId, String menuItemName, int quantity, BigDecimal price) {
        OrderItem orderItem = new OrderItem();
        orderItem.setId(id);
        orderItem.setMenuItemId(menuItemId);
        orderItem.setMenuItemName(menuItemName);
        orderItem.setQuantity(quantity);
        orderItem.setPrice(price);
        orderItem.setStatus(OrderItemStatus.PENDING);
        return orderItem;
    }
}
