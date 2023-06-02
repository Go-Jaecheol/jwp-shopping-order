package cart.application;

import cart.domain.Member;
import cart.domain.MemberCoupon;
import cart.domain.Order;
import cart.domain.Product;
import cart.domain.Products;
import cart.domain.vo.Amount;
import cart.dto.CartItemRequest;
import cart.dto.OrderProductResponse;
import cart.dto.OrderRequest;
import cart.dto.OrderResponse;
import cart.exception.BusinessException;
import cart.repository.MemberCouponRepository;
import cart.repository.OrderRepository;
import cart.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberCouponRepository memberCouponRepository;

    public OrderService(final OrderRepository orderRepository, final ProductRepository productRepository,
                        final MemberCouponRepository memberCouponRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.memberCouponRepository = memberCouponRepository;
    }

    public OrderResponse order(final OrderRequest orderRequest, final Member member) {
        final List<Product> products = findProducts(orderRequest);
        final MemberCoupon coupon = memberCouponRepository.findByCouponIdAndMemberId(orderRequest.getCouponId(),
                member.getId());

        final Order order = orderRepository.create(
                new Order(new Products(products), coupon, Amount.of(orderRequest.getDeliveryAmount()),
                        orderRequest.getAddress()), member.getId());
        coupon.use();
        memberCouponRepository.update(coupon, member.getId());
        final List<OrderProductResponse> orderProductResponses = makeOrderProductResponses(orderRequest,
                products);
        return new OrderResponse(order.getId(), orderRequest.getTotalAmount(),
                order.getDeliveryAmount().getValue(), order.discountProductAmount().getValue(), order.getAddress(),
                orderProductResponses);
    }

    private List<Product> findProducts(final OrderRequest orderRequest) {
        final List<Product> products = new ArrayList<>();
        final List<Amount> amounts = orderRequest.getProducts().stream()
                .map(it -> {
                    final Product product = productRepository.findById(it.getProductId());
                    products.add(product);
                    return product.getAmount().multiply(it.getQuantity());
                })
                .collect(Collectors.toList());
        final Amount sum = Amount.of(amounts);
        if (!sum.equals(Amount.of(orderRequest.getTotalAmount()))) {
            throw new BusinessException("상품 금액이 변경되었습니다.");
        }
        return products;
    }

    private List<OrderProductResponse> makeOrderProductResponses(final OrderRequest orderRequest,
                                                                 final List<Product> products) {
        final List<OrderProductResponse> orderProductResponses = new ArrayList<OrderProductResponse>();
        for (int index = 0; index < products.size(); index++) {
            final Product product = products.get(index);
            final CartItemRequest cartItemRequest = orderRequest.getProducts().get(index);
            final OrderProductResponse orderProductResponse = new OrderProductResponse(product.getId(),
                    product.getName(), product.getAmount().getValue(),
                    product.getImageUrl(), cartItemRequest.getQuantity());
            orderProductResponses.add(orderProductResponse);
        }
        return orderProductResponses;
    }
}
