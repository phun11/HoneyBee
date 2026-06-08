@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Transactional
    public String processOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        if (!order.getStatus().equals("PENDING")) {
            return "Đơn hàng đã được xử lý trước đó";
        }

        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findById(item.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

            Inventory inventory = inventoryRepository.findByProductId(product.getId());

            if (inventory.getQuantity() < item.getQuantity()) {
                Supplier supplier = supplierRepository.findByProductId(product.getId());
                System.out.println("Đặt thêm hàng từ nhà cung cấp: " + supplier.getName());
                return "Không đủ hàng trong kho, đã gửi yêu cầu nhập thêm";
            }

            inventory.setQuantity(inventory.getQuantity() - item.getQuantity());
            inventoryRepository.save(inventory);
        }

        order.setStatus("COMPLETED");
        order.setProcessedAt(LocalDateTime.now());
        orderRepository.save(order);

        System.out.println("Đơn hàng " + orderId + " đã được xử lý thành công");

        return "Xử lý đơn hàng thành công";
    }
}