package com.gufrancode.customer;

import com.gufrancode.amqp.RabbitMQMessageProducer;
import com.gufrancode.clients.fraud.FraudCheckResponse;
import com.gufrancode.clients.fraud.FraudClient;
import com.gufrancode.clients.notification.NotificationClient;
import com.gufrancode.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final RestTemplate restTemplate;

    private final FraudClient fraudClient;
    private final NotificationClient notificationClient;

    private final RabbitMQMessageProducer rabbitMQMessageProducer;
    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .build();
        customerRepository.saveAndFlush(customer);
     //   FraudCheckResponse fraudCheckResponse = restTemplate.getForObject("http://FRAUD/api/v1/fraud-check/{customerId}",FraudCheckResponse.class,customer.getId());

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

//        if(!fraudCheckResponse.isFraudster()){
//              notificationClient.sendNotification(new NotificationRequest(customer.getId(), customer.getFirstName(),
//                      String.format("Hi %s, welcome to Amigoscode...",customer.getFirstName())));
//
//        }

        if(fraudCheckResponse.isFraudster()){
            throw new IllegalStateException("fraudster");
        }

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to Amigoscode...",
                        customer.getFirstName())
        );
        rabbitMQMessageProducer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification.routing-key"
        );
    }
}
