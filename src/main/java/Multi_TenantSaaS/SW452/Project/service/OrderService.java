package Multi_TenantSaaS.SW452.Project.service;

import Multi_TenantSaaS.SW452.Project.entity.Order;
import Multi_TenantSaaS.SW452.Project.entity.Product;
import Multi_TenantSaaS.SW452.Project.exception.BusinessException;
import Multi_TenantSaaS.SW452.Project.repository.OrderRepository;
import Multi_TenantSaaS.SW452.Project.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order createOrder(Long productId, int quantity) {

        Product product = productRepository
                .findById(productId)
                .orElseThrow(() -> new BusinessException("Product not found"));

        if(product.getStock() < quantity) {
            throw new BusinessException("Not enough stock");
        }

        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        Order order = new Order();
        order.setProduct(product);
        order.setQuantity(quantity);

        if(quantity > 5) {
            throw new RuntimeException("Demo rollback error");
        }

        return orderRepository.save(order);
    }
}
