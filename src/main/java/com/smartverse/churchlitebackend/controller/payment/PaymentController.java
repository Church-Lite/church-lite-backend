package com.smartverse.churchlitebackend.controller.payment;

import com.smartverse.churchlitebackend.service.payment.PaymentCheckoutService;
import com.smartverse.churchlitebackend_gen.endpoints.CreatePaymentLink;
import com.smartverse.churchlitebackend_gen.endpoints.CreatePaymentLinkInput;
import com.smartverse.churchlitebackend_gen.endpoints.CreatePaymentLinkOutput;
import com.smartverse.churchlitebackend_gen.endpoints.GetPaymentHistory;
import com.smartverse.churchlitebackend_gen.endpoints.GetPaymentHistoryOutput;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController implements CreatePaymentLink, GetPaymentHistory {
    private final PaymentCheckoutService checkoutService;
    private final HttpServletRequest request;

    public PaymentController(
            PaymentCheckoutService checkoutService,
            HttpServletRequest request) {
        this.checkoutService = checkoutService;
        this.request = request;
    }

    @Override
    public ResponseEntity<CreatePaymentLinkOutput> createPaymentLink(CreatePaymentLinkInput input) {
        return ResponseEntity.ok(checkoutService.createLink(
                input.planCode,
                input.billingCycle,
                request.getHeader("Authorization")));
    }

    @Override
    public ResponseEntity<GetPaymentHistoryOutput> getPaymentHistory() {
        return ResponseEntity.ok(checkoutService.history());
    }
}
