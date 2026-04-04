package Multi_TenantSaaS.SW452.Project.config;

import Multi_TenantSaaS.SW452.Project.entity.Product;
import Multi_TenantSaaS.SW452.Project.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductInitializer implements CommandLineRunner {

    private final ProductRepository productRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productRepository.count() == 0) {
            Product product1 = new Product();
            product1.setName("Laptop");
            product1.setStock(20);
            productRepository.save(product1);

            Product product2 = new Product();
            product2.setName("Mouse");
            product2.setStock(30);
            productRepository.save(product2);

            Product product3 = new Product();
            product3.setName("Keyboard");
            product3.setStock(15);
            productRepository.save(product3);

            System.out.println("Sample products created successfully!");
        }
    }
}
