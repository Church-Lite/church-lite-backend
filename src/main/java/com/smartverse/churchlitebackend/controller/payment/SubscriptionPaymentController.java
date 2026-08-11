package com.smartverse.churchlitebackend.controller.payment;
import com.smartverse.churchlitebackend.service.payment.SubscriptionPaymentBusinessService;
import com.smartverse.churchlitebackend_gen.endpoints.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;import org.springframework.web.bind.annotation.RestController;
@RestController public class SubscriptionPaymentController implements CreateSubscriptionPaymentLink{
 private final SubscriptionPaymentBusinessService service;private final HttpServletRequest request;
 public SubscriptionPaymentController(SubscriptionPaymentBusinessService s,HttpServletRequest r){service=s;request=r;}
 public ResponseEntity<CreateSubscriptionPaymentLinkOutput> createSubscriptionPaymentLink(CreateSubscriptionPaymentLinkInput i){var c=service.checkout(i.planCode,i.billingCycle,request.getHeader(HttpHeaders.AUTHORIZATION));var o=new CreateSubscriptionPaymentLinkOutput();o.url=c.url();o.orderNsu=c.orderNsu();o.status=c.status();o.reused=c.reused();return ResponseEntity.ok(o);}
}
